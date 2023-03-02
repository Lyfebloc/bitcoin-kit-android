package com.lyfebloc.bitcoincore.transactions

import com.lyfebloc.bitcoincore.core.IPluginData
import com.lyfebloc.bitcoincore.core.IPublicKeyManager
import com.lyfebloc.bitcoincore.core.IRecipientSetter
import com.lyfebloc.bitcoincore.models.TransactionDataSortType
import com.lyfebloc.bitcoincore.transactions.builder.InputSetter
import com.lyfebloc.bitcoincore.transactions.builder.MutableTransaction
import com.lyfebloc.bitcoincore.transactions.scripts.ScriptType
import com.lyfebloc.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
    private val recipientSetter: IRecipientSetter,
    private val inputSetter: InputSetter,
    private val addressConverter: AddressConverterChain,
    private val publicKeyManager: IPublicKeyManager,
    private val changeScriptType: ScriptType
) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: String?, pluginData: Map<Byte, IPluginData>): Long {
        val mutableTransaction = MutableTransaction()

        recipientSetter.setRecipient(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData, true)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay, TransactionDataSortType.None)

        val inputsTotalValue = mutableTransaction.inputsToSign.map { it.previousOutput.value }.sum()
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return inputsTotalValue - outputsTotalValue
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(publicKey = publicKeyManager.changePublicKey(), scriptType = changeScriptType).string
    }
}
