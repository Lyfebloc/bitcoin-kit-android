package com.lyfebloc.bitcoincore.network.peer.task

import com.lyfebloc.bitcoincore.network.messages.GetHeadersMessage
import com.lyfebloc.bitcoincore.network.messages.HeadersMessage
import com.lyfebloc.bitcoincore.network.messages.IMessage
import com.lyfebloc.bitcoincore.storage.BlockHeader

class GetBlockHeadersTask(private val blockLocatorHashes: List<ByteArray>) : PeerTask() {

    var blockHeaders = arrayOf<BlockHeader>()

    override fun start() {
        requester?.let { it.send(GetHeadersMessage(it.protocolVersion, blockLocatorHashes, ByteArray(32))) }
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is HeadersMessage) {
            return false
        }

        blockHeaders = message.headers
        listener?.onTaskCompleted(this)

        return true
    }

    override fun handleTimeout() {
        listener?.onTaskCompleted(this)
    }
}
