package com.lyfebloc.bitcoincore.network.messages

class UnknownMessage(val command: String) : IMessage {
    override fun toString(): String {
        return "UnknownMessage(command=$command)"
    }
}
