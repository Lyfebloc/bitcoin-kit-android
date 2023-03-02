package com.lyfebloc.bitcoincore.managers

import com.lyfebloc.bitcoincore.storage.UnspentOutput

interface IUnspentOutputProvider {
    fun getSpendableUtxo(): List<UnspentOutput>
}
