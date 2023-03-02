package com.lyfebloc.bitcoincore.transactions

import com.lyfebloc.bitcoincore.WatchedTransactionManager
import com.lyfebloc.bitcoincore.blocks.IBlockchainDataListener
import com.lyfebloc.bitcoincore.core.IPublicKeyManager
import com.lyfebloc.bitcoincore.core.IStorage
import com.lyfebloc.bitcoincore.core.inTopologicalOrder
import com.lyfebloc.bitcoincore.managers.BloomFilterManager
import com.lyfebloc.bitcoincore.managers.IIrregularOutputFinder
import com.lyfebloc.bitcoincore.managers.PublicKeyManager
import com.lyfebloc.bitcoincore.models.Block
import com.lyfebloc.bitcoincore.models.Transaction
import com.lyfebloc.bitcoincore.storage.FullTransaction
import com.lyfebloc.bitcoincore.transactions.extractors.TransactionExtractor

class BlockTransactionProcessor(
        private val storage: IStorage,
        private val extractor: TransactionExtractor,
        private val publicKeyManager: IPublicKeyManager,
        private val irregularOutputFinder: IIrregularOutputFinder,
        private val dataListener: IBlockchainDataListener,
        private val conflictsResolver: TransactionConflictsResolver,
        private val invalidator: TransactionInvalidator) {

    var transactionListener: WatchedTransactionManager? = null

    @Throws(BloomFilterManager.BloomFilterExpired::class)
    fun processReceived(transactions: List<FullTransaction>, block: Block, skipCheckBloomFilter: Boolean) {
        var needToUpdateBloomFilter = false

        val inserted = mutableListOf<Transaction>()
        val updated = mutableListOf<Transaction>()

        // when the same transaction came in merkle block and from another peer's mempool we need to process it serial
        synchronized(this) {
            for ((index, fullTransaction) in transactions.inTopologicalOrder().withIndex()) {
                val transaction = fullTransaction.header
                val existingTransaction = storage.getFullTransaction(transaction.hash)
                if (existingTransaction != null) {
                    extractor.extract(existingTransaction)
                    transactionListener?.onTransactionReceived(existingTransaction)
                    relay(existingTransaction.header, index, block)

                    storage.updateTransaction(existingTransaction)
                    updated.add(existingTransaction.header)

                    continue
                }

                extractor.extract(fullTransaction)
                transactionListener?.onTransactionReceived(fullTransaction)

                if (!transaction.isMine) {
                    conflictsResolver.getIncomingPendingTransactionsConflictingWith(fullTransaction).forEach { tx ->
                        tx.conflictingTxHash = fullTransaction.header.hash
                        invalidator.invalidate(tx)
                        needToUpdateBloomFilter = true
                    }

                    continue
                }

                relay(transaction, index, block)

                conflictsResolver.getTransactionsConflictingWithInBlockTransaction(fullTransaction).forEach {
                    it.conflictingTxHash = fullTransaction.header.hash
                    invalidator.invalidate(it)
                }


                val invalidTransaction = storage.getInvalidTransaction(transaction.hash)
                if (invalidTransaction != null) {
                    storage.moveInvalidTransactionToTransactions(invalidTransaction, fullTransaction)
                    updated.add(transaction)
                } else {
                    storage.addTransaction(fullTransaction)
                    inserted.add(transaction)
                }

                if (!skipCheckBloomFilter) {
                    needToUpdateBloomFilter = needToUpdateBloomFilter ||
                            publicKeyManager.gapShifts() ||
                            irregularOutputFinder.hasIrregularOutput(fullTransaction.outputs)
                }
            }
        }

        if (inserted.isNotEmpty() || updated.isNotEmpty()) {
            if (!block.hasTransactions) {
                block.hasTransactions = true
                storage.updateBlock(block)

            }
            dataListener.onTransactionsUpdate(inserted, updated, block)
        }

        if (needToUpdateBloomFilter) {
            throw BloomFilterManager.BloomFilterExpired
        }
    }


    private fun relay(transaction: Transaction, order: Int, block: Block) {
        transaction.blockHash = block.headerHash
        transaction.timestamp = block.timestamp
        transaction.conflictingTxHash = null
        transaction.status = Transaction.Status.RELAYED
        transaction.order = order
    }

}
