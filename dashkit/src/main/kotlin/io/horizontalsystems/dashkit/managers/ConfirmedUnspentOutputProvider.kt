package com.lyfebloc.dashkit.managers

import com.lyfebloc.bitcoincore.core.IStorage
import com.lyfebloc.bitcoincore.managers.IUnspentOutputProvider
import com.lyfebloc.bitcoincore.storage.UnspentOutput

class ConfirmedUnspentOutputProvider(private val storage: IStorage, private val confirmationsThreshold: Int) : IUnspentOutputProvider {
    override fun getSpendableUtxo(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        return storage.getUnspentOutputs().filter { isOutputConfirmed(it, lastBlockHeight) }
    }

    private fun isOutputConfirmed(unspentOutput: UnspentOutput, lastBlockHeight: Int): Boolean {
        val block = unspentOutput.block ?: return false

        return block.height <= lastBlockHeight - confirmationsThreshold + 1
    }
}
