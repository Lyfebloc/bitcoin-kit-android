package com.lyfebloc.bitcoincore.core

import com.lyfebloc.bitcoincore.managers.IRestoreKeyConverter
import com.lyfebloc.bitcoincore.models.Address
import com.lyfebloc.bitcoincore.models.PublicKey
import com.lyfebloc.bitcoincore.models.TransactionOutput
import com.lyfebloc.bitcoincore.storage.FullTransaction
import com.lyfebloc.bitcoincore.storage.UnspentOutput
import com.lyfebloc.bitcoincore.transactions.builder.MutableTransaction
import com.lyfebloc.bitcoincore.transactions.scripts.Script

class PluginManager : IRestoreKeyConverter {
    private val plugins = mutableMapOf<Byte, IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: Map<Byte, IPluginData>, skipChecking: Boolean) {
        pluginData.forEach {
            val plugin = checkNotNull(plugins[it.key])
            plugin.processOutputs(mutableTransaction, it.value, skipChecking)
        }
    }

    fun processInputs(mutableTransaction: MutableTransaction) {
        for (inputToSign in mutableTransaction.inputsToSign) {
            val pluginId = inputToSign.previousOutput.pluginId ?: continue
            val plugin = checkNotNull(plugins[pluginId])
            inputToSign.input.sequence = plugin.getInputSequence(inputToSign.previousOutput)
        }
    }

    fun addPlugin(plugin: IPlugin) {
        plugins[plugin.id] = plugin
    }

    fun processTransactionWithNullData(transaction: FullTransaction, nullDataOutput: TransactionOutput) {
        val script = Script(nullDataOutput.lockingScript)
        val nullDataChunksIterator = script.chunks.iterator()

        // the first byte OP_RETURN
        nullDataChunksIterator.next()

        while (nullDataChunksIterator.hasNext()) {
            val pluginId = nullDataChunksIterator.next()
            val plugin = plugins[pluginId.opcode.toByte()] ?: break

            try {
                plugin.processTransactionWithNullData(transaction, nullDataChunksIterator)
            } catch (e: Exception) {

            }
        }
    }

    /**
     * Tell if UTXO is spendable using the corresponding plugin
     *
     * @return true if pluginId is null, false if no plugin found for pluginId,
     * otherwise delegate it to corresponding plugin
     */
    fun isSpendable(unspentOutput: UnspentOutput): Boolean {
        val pluginId = unspentOutput.output.pluginId ?: return true
        val plugin = plugins[pluginId] ?: return false
        return plugin.isSpendable(unspentOutput)
    }

    fun parsePluginData(output: TransactionOutput, txTimestamp: Long): IPluginOutputData? {
        val plugin = plugins[output.pluginId] ?: return null

        return try {
            plugin.parsePluginData(output, txTimestamp)
        } catch (e: Exception) {
            null
        }
    }

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return plugins.map { it.value.keysForApiRestore(publicKey) }.flatten().distinct()
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf()
    }

    fun validateAddress(address: Address, pluginData: Map<Byte, IPluginData>) {
        pluginData.forEach {
            val plugin = checkNotNull(plugins[it.key])

            plugin.validateAddress(address)
        }
    }
}

interface IPlugin {
    val id: Byte

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: IPluginData, skipChecking: Boolean)
    fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>)
    fun isSpendable(unspentOutput: UnspentOutput): Boolean
    fun getInputSequence(output: TransactionOutput): Long
    fun parsePluginData(output: TransactionOutput, txTimestamp: Long): IPluginOutputData
    fun keysForApiRestore(publicKey: PublicKey): List<String>
    fun validateAddress(address: Address)
}

interface IPluginData
interface IPluginOutputData
