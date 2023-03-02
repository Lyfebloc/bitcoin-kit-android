package com.lyfebloc.bitcoincore.transactions

import com.lyfebloc.bitcoincore.WatchedTransactionManager
import com.lyfebloc.bitcoincore.blocks.IBlockchainDataListener
import com.lyfebloc.bitcoincore.core.IPublicKeyManager
import com.lyfebloc.bitcoincore.core.IStorage
import com.lyfebloc.bitcoincore.core.inTopologicalOrder
import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.managers.BloomFilterManager
import com.lyfebloc.bitcoincore.managers.IIrregularOutputFinder
import com.lyfebloc.bitcoincore.managers.PublicKeyManager
import com.lyfebloc.bitcoincore.models.Transaction
import com.lyfebloc.bitcoincore.storage.FullTransaction
import com.lyfebloc.bitcoincore.transactions.extractors.TransactionExtractor

class PendingTransactionProcessor(
    private val storage: IStorage,
    private val extractor: TransactionExtractor,
    private val publicKeyManager: IPublicKeyManager,
    private val irregularOutputFinder: IIrregularOutputFinder,
    private val dataListener: IBlockchainDataListener,
    private val conflictsResolver: TransactionConflictsResolver) {

    private val notMineTransactions = HashSet<ByteArray>()

    var transactionListener: WatchedTransactionManager? = null

    fun processCreated(transaction: FullTransaction) {
        if (storage.getTransaction(transaction.header.hash) != null) {
            throw TransactionCreator.TransactionAlreadyExists("hash = ${transaction.header.hash.toReversedHex()}")
        }

        extractor.extract(transaction)
        storage.addTransaction(transaction)

        try {
            dataListener.onTransactionsUpdate(listOf(transaction.header), listOf(), null)
        } catch (e: Exception) {
            // ignore any exception since the tx is inserted to the db
        }

        if (irregularOutputFinder.hasIrregularOutput(transaction.outputs)) {
            throw BloomFilterManager.BloomFilterExpired
        }
    }

    @Throws(BloomFilterManager.BloomFilterExpired::class)
    fun processReceived(transactions: List<FullTransaction>, skipCheckBloomFilter: Boolean) {
        var needToUpdateBloomFilter = false

        val inserted = mutableListOf<Transaction>()
        val updated = mutableListOf<Transaction>()

        // when the same transaction came in merkle block and from another peer's mempool we need to process it serial
        synchronized(this) {
            for ((index, transaction) in transactions.inTopologicalOrder().withIndex()) {
                if (notMineTransactions.any { it.contentEquals(transaction.header.hash) }) {
                    // already processed this transaction with same state
                    continue
                }

                val invalidTransaction = storage.getInvalidTransaction(transaction.header.hash)
                if (invalidTransaction != null) {
                    // if some peer send us transaction after it's invalidated, we must ignore it
                    continue
                }

                val existingTransaction = storage.getTransaction(transaction.header.hash)
                if (existingTransaction != null) {
                    if (existingTransaction.status == Transaction.Status.RELAYED) {
                        // if comes again from memPool we don't need to update it
                        continue
                    }

                    relay(existingTransaction, index)

                    storage.updateTransaction(existingTransaction)
                    updated.add(existingTransaction)

                    continue
                }

                relay(transaction.header, index)
                extractor.extract(transaction)
                transactionListener?.onTransactionReceived(transaction)

                if (!transaction.header.isMine) {
                    notMineTransactions.add(transaction.header.hash)

                    conflictsResolver.getIncomingPendingTransactionsConflictingWith(transaction).forEach { tx ->
                        // Former incoming transaction is conflicting with current transaction
                        tx.conflictingTxHash = transaction.header.hash
                        storage.updateTransaction(tx)
                        updated.add(tx)
                    }

                    continue
                }



                val conflictingTransactions = conflictsResolver.getTransactionsConflictingWithPendingTransaction(transaction)
                if (conflictingTransactions.isNotEmpty()) {
                    // Ignore current transaction and mark former transactions as conflicting with current transaction
                    conflictingTransactions.forEach { tx ->
                        tx.conflictingTxHash = transaction.header.hash
                        storage.updateTransaction(tx)
                        updated.add(tx)
                    }
                } else {
                    storage.addTransaction(transaction)
                    inserted.add(transaction.header)
                }

                if (!skipCheckBloomFilter) {
                    val checkDoubleSpend = !transaction.header.isOutgoing
                    needToUpdateBloomFilter = needToUpdateBloomFilter ||
                            checkDoubleSpend ||
                            publicKeyManager.gapShifts() ||
                            irregularOutputFinder.hasIrregularOutput(transaction.outputs)
                }
            }
        }

        if (inserted.isNotEmpty() || updated.isNotEmpty()) {
            dataListener.onTransactionsUpdate(inserted, updated, null)
        }

        if (needToUpdateBloomFilter) {
            throw BloomFilterManager.BloomFilterExpired
        }
    }

    private fun relay(transaction: Transaction, order: Int) {
        transaction.status = Transaction.Status.RELAYED
        transaction.order = order
    }
}
