package com.lyfebloc.bitcoincore.transactions.builder

import com.lyfebloc.bitcoincore.core.IPrivateWallet
import com.lyfebloc.bitcoincore.models.Transaction
import com.lyfebloc.bitcoincore.models.TransactionOutput
import com.lyfebloc.bitcoincore.network.Network
import com.lyfebloc.bitcoincore.serializers.TransactionSerializer
import com.lyfebloc.bitcoincore.storage.InputToSign
import com.lyfebloc.bitcoincore.transactions.scripts.ScriptType
import com.lyfebloc.hdwalletkit.HDWallet

class InputSigner(private val hdWallet: IPrivateWallet, val network: Network) {

    fun sigScriptData(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, index: Int): List<ByteArray> {

        val input = inputsToSign[index]
        val prevOutput = input.previousOutput
        val publicKey = input.previousOutputPublicKey

        val privateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external)) {
            throw Error.NoPrivateKey()
        }

        val txContent = TransactionSerializer.serializeForSignature(transaction, inputsToSign, outputs, index, prevOutput.scriptType.isWitness || network.sigHashForked) + byteArrayOf(network.sigHashValue, 0, 0, 0)
        val signature = privateKey.createSignature(txContent) + network.sigHashValue

        return when (prevOutput.scriptType) {
            ScriptType.P2PK -> listOf(signature)
            else -> listOf(signature, publicKey.publicKey)
        }
    }

    open class Error : Exception() {
        class NoPrivateKey : Error()
        class NoPreviousOutput : Error()
        class NoPreviousOutputAddress : Error()
    }
}
