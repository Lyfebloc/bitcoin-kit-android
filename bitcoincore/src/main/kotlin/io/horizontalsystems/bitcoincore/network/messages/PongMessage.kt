package com.lyfebloc.bitcoincore.network.messages

import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.io.BitcoinOutput

class PongMessage(val nonce: Long) : IMessage {
    override fun toString(): String {
        return "PongMessage(nonce=$nonce)"
    }
}

class PongMessageParser : IMessageParser {
    override val command: String = "pong"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        return PongMessage(input.readLong())
    }
}

class PongMessageSerializer : IMessageSerializer {
    override val command: String = "pong"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is PongMessage) {
            return null
        }

        return BitcoinOutput()
                .writeLong(message.nonce)
                .toByteArray()
    }
}
