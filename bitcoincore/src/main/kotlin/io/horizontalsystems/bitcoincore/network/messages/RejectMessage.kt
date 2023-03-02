package com.lyfebloc.bitcoincore.network.messages

import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable

class RejectMessage(private val responseToMessage: String,
                    private val rejectCode: Byte,
                    private val reason: String) : IMessage {

    override fun toString(): String {
        return "RejectMessage(responseToMessage=$responseToMessage, rejectCode: $rejectCode, reason: $reason)"
    }
}

class RejectMessageParser : IMessageParser {
    override val command = "reject"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val responseToMessage = input.readString()
        val rejectCode = input.readByte()
        val reason = input.readString()

        return RejectMessage(responseToMessage, rejectCode, reason)
    }

}
