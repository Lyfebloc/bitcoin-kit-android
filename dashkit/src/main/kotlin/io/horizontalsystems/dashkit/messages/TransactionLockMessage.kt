package com.lyfebloc.dashkit.messages

import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.network.messages.IMessage
import com.lyfebloc.bitcoincore.network.messages.IMessageParser
import com.lyfebloc.bitcoincore.serializers.TransactionSerializer
import com.lyfebloc.bitcoincore.storage.FullTransaction

class TransactionLockMessage(var transaction: FullTransaction) : IMessage {
    override fun toString(): String {
        return "TransactionLockMessage(${transaction.header.hash.toReversedHex()})"
    }
}

class TransactionLockMessageParser : IMessageParser {
    override val command: String = "ix"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val transaction = TransactionSerializer.deserialize(input)
        return TransactionLockMessage(transaction)
    }
}
