package com.lyfebloc.dashkit.messages

import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.io.BitcoinOutput
import com.lyfebloc.bitcoincore.network.messages.IMessage
import com.lyfebloc.bitcoincore.network.messages.IMessageParser
import com.lyfebloc.bitcoincore.utils.HashUtils

class TransactionLockVoteMessage(
        var txHash: ByteArray,
        var outpoint: Outpoint,
        var outpointMasternode: Outpoint,
        var quorumModifierHash: ByteArray,
        var masternodeProTxHash: ByteArray,
        var vchMasternodeSignature: ByteArray,
        var hash: ByteArray) : IMessage {

    override fun toString(): String {
        return "TransactionLockVoteMessage(hash=${hash.toReversedHex()}, txHash=${txHash.toReversedHex()})"
    }

}

class Outpoint(val txHash: ByteArray, val vout: Long) {
    constructor(input: BitcoinInputMarkable) : this(input.readBytes(32), input.readUnsignedInt())
}

class TransactionLockVoteMessageParser : IMessageParser {
    override val command: String = "txlvote"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val txHash = input.readBytes(32)
        val outpoint = Outpoint(input)
        val outpointMasternode = Outpoint(input)
        val quorumModifierHash = input.readBytes(32)
        val masternodeProTxHash = input.readBytes(32)
        val signatureLength = input.readVarInt()
        val vchMasternodeSignature = ByteArray(signatureLength.toInt())
        input.readFully(vchMasternodeSignature)

        val hashPayload = BitcoinOutput()
                .write(txHash)
                .write(outpoint.txHash)
                .writeUnsignedInt(outpoint.vout)
                .write(outpointMasternode.txHash)
                .writeUnsignedInt(outpointMasternode.vout)
                .write(quorumModifierHash)
                .write(masternodeProTxHash)
                .toByteArray()

        val hash = HashUtils.doubleSha256(hashPayload)

        return TransactionLockVoteMessage(txHash, outpoint, outpointMasternode, quorumModifierHash, masternodeProTxHash, vchMasternodeSignature, hash)
    }
}
