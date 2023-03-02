package com.lyfebloc.bitcoincore.network.messages

import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.serializers.TransactionSerializer
import com.lyfebloc.bitcoincore.storage.FullTransaction

class TransactionMessage(var transaction: FullTransaction, val size: Int) : IMessage {
    override fun toString(): String {
        return "TransactionMessage(${transaction.header.hash.toReversedHex()})"
    }
}

class TransactionMessageParser : IMessageParser {
    override val command: String = "tx"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val transaction = TransactionSerializer.deserialize(input)
        return TransactionMessage(transaction, input.count)
    }
}

class TransactionMessageSerializer : IMessageSerializer {
    override val command: String = "tx"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is TransactionMessage) {
            return null
        }

        return TransactionSerializer.serialize(message.transaction)
    }
}
