package com.lyfebloc.bitcoincore.network.peer.task

import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.models.InventoryItem
import com.lyfebloc.bitcoincore.network.messages.GetDataMessage
import com.lyfebloc.bitcoincore.network.messages.IMessage
import com.lyfebloc.bitcoincore.network.messages.InvMessage
import com.lyfebloc.bitcoincore.network.messages.TransactionMessage
import com.lyfebloc.bitcoincore.storage.FullTransaction
import java.util.concurrent.TimeUnit

class SendTransactionTask(val transaction: FullTransaction) : PeerTask() {

    init {
        allowedIdleTime = TimeUnit.SECONDS.toMillis(30)
    }

    override val state: String
        get() = "transaction: ${transaction.header.hash.toReversedHex()}"

    override fun start() {
        requester?.send(InvMessage(InventoryItem.MSG_TX, transaction.header.hash))
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        val transactionRequested =
                message is GetDataMessage &&
                message.inventory.any { it.type == InventoryItem.MSG_TX && it.hash.contentEquals(transaction.header.hash) }

        if (transactionRequested) {
            requester?.send(TransactionMessage(transaction, 0))
            listener?.onTaskCompleted(this)
        }

        return transactionRequested
    }

    override fun handleTimeout() {
        listener?.onTaskCompleted(this)
    }

}
