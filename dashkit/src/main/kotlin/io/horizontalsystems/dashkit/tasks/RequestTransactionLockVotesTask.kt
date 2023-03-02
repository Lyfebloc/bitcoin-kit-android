package com.lyfebloc.dashkit.tasks

import com.lyfebloc.bitcoincore.models.InventoryItem
import com.lyfebloc.bitcoincore.network.messages.GetDataMessage
import com.lyfebloc.bitcoincore.network.messages.IMessage
import com.lyfebloc.bitcoincore.network.peer.task.PeerTask
import com.lyfebloc.dashkit.InventoryType
import com.lyfebloc.dashkit.messages.TransactionLockVoteMessage

class RequestTransactionLockVotesTask(hashes: List<ByteArray>) : PeerTask() {

    val hashes = hashes.toMutableList()
    var transactionLockVotes = mutableListOf<TransactionLockVoteMessage>()

    override fun start() {
        val items = hashes.map { hash ->
            InventoryItem(InventoryType.MSG_TXLOCK_VOTE, hash)
        }

        requester?.send(GetDataMessage(items))
    }

    override fun handleMessage(message: IMessage) = when (message) {
        is TransactionLockVoteMessage -> handleTransactionLockVote(message)
        else -> false
    }

    private fun handleTransactionLockVote(transactionLockVote: TransactionLockVoteMessage): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(transactionLockVote.hash) } ?: return false

        hashes.remove(hash)
        transactionLockVotes.add(transactionLockVote)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
