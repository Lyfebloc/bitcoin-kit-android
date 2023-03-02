package com.lyfebloc.bitcoincore.transactions.builder

import com.lyfebloc.bitcoincore.core.IPluginData
import com.lyfebloc.bitcoincore.core.IRecipientSetter
import com.lyfebloc.bitcoincore.core.PluginManager
import com.lyfebloc.bitcoincore.transactions.builder.MutableTransaction
import com.lyfebloc.bitcoincore.utils.IAddressConverter

class RecipientSetter(
        private val addressConverter: IAddressConverter,
        private val pluginManager: PluginManager
) : IRecipientSetter {

    override fun setRecipient(mutableTransaction: MutableTransaction, toAddress: String, value: Long, pluginData: Map<Byte, IPluginData>, skipChecking: Boolean) {
        mutableTransaction.recipientAddress = addressConverter.convert(toAddress)
        mutableTransaction.recipientValue = value

        pluginManager.processOutputs(mutableTransaction, pluginData, skipChecking)
    }

}
