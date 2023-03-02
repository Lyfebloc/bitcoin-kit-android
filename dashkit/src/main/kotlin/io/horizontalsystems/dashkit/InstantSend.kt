package com.lyfebloc.dashkit

import com.lyfebloc.bitcoincore.models.InventoryItem
import com.lyfebloc.bitcoincore.network.peer.IInventoryItemsHandler
import com.lyfebloc.bitcoincore.network.peer.IPeerTaskHandler
import com.lyfebloc.bitcoincore.network.peer.Peer
import com.lyfebloc.bitcoincore.network.peer.task.PeerTask
import com.lyfebloc.bitcoincore.storage.FullTransaction
import com.lyfebloc.bitcoincore.transactions.TransactionSyncer
import com.lyfebloc.dashkit.instantsend.instantsendlock.InstantSendLockHandler
import com.lyfebloc.dashkit.instantsend.transactionlockvote.TransactionLockVoteHandler
import com.lyfebloc.dashkit.messages.ISLockMessage
import com.lyfebloc.dashkit.messages.TransactionLockVoteMessage
import com.lyfebloc.dashkit.tasks.RequestInstantSendLocksTask
import com.lyfebloc.dashkit.tasks.RequestTransactionLockRequestsTask
import com.lyfebloc.dashkit.tasks.RequestTransactionLockVotesTask
import java.util.concurrent.Executors

class InstantSend(
        private val transactionSyncer: TransactionSyncer,
        private val transactionLockVoteHandler: TransactionLockVoteHandler,
        private val instantSendLockHandler: InstantSendLockHandler
) : IInventoryItemsHandler, IPeerTaskHandler {

    private val dispatchQueue = Executors.newSingleThreadExecutor()

    fun handle(insertedTxHash: ByteArray) {
        instantSendLockHandler.handle(insertedTxHash)
    }

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        val transactionLockRequests = mutableListOf<ByteArray>()
        val transactionLockVotes = mutableListOf<ByteArray>()
        val isLocks = mutableListOf<ByteArray>()

        inventoryItems.forEach { item ->
            when (item.type) {
                InventoryType.MSG_TXLOCK_REQUEST -> {
                    transactionLockRequests.add(item.hash)
                }
                InventoryType.MSG_TXLOCK_VOTE -> {
                    transactionLockVotes.add(item.hash)
                }
                InventoryType.MSG_ISLOCK -> {
                    isLocks.add(item.hash)
                }
            }
        }

        if (transactionLockRequests.isNotEmpty()) {
            peer.addTask(RequestTransactionLockRequestsTask(transactionLockRequests))
        }

        if (transactionLockVotes.isNotEmpty()) {
            peer.addTask(RequestTransactionLockVotesTask(transactionLockVotes))
        }

        if (isLocks.isNotEmpty()) {
            peer.addTask(RequestInstantSendLocksTask(isLocks))
        }

    }

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is RequestTransactionLockRequestsTask -> {
                dispatchQueue.execute {
                    handleTransactions(task.transactions)
                }
                true
            }
            is RequestTransactionLockVotesTask -> {
                dispatchQueue.execute {
                    handleTransactionLockVotes(task.transactionLockVotes)
                }
                true
            }
            is RequestInstantSendLocksTask -> {
                dispatchQueue.execute {
                    handleInstantSendLocks(task.isLocks)
                }
                true
            }
            else -> false
        }
    }

    private fun handleTransactions(transactions: List<FullTransaction>) {
        transactionSyncer.handleRelayed(transactions)

        for (transaction in transactions) {
            transactionLockVoteHandler.handle(transaction)
        }
    }

    private fun handleTransactionLockVotes(transactionLockVotes: List<TransactionLockVoteMessage>) {
        for (vote in transactionLockVotes) {
            transactionLockVoteHandler.handle(vote)
        }
    }

    private fun handleInstantSendLocks(isLocks: List<ISLockMessage>) {
        for (isLock in isLocks) {
            instantSendLockHandler.handle(isLock)
        }
    }

    companion object {
        const val requiredVoteCount = 6
    }

}
