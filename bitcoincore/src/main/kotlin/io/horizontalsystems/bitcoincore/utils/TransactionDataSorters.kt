package com.lyfebloc.bitcoincore.utils

import com.lyfebloc.bitcoincore.core.ITransactionDataSorter
import com.lyfebloc.bitcoincore.models.TransactionOutput
import com.lyfebloc.bitcoincore.storage.UnspentOutput
import java.util.*

class Bip69Sorter : ITransactionDataSorter {
    override fun sortOutputs(outputs: List<TransactionOutput>): List<TransactionOutput> {
        Collections.sort(outputs, Bip69.outputComparator)
        return outputs
    }

    override fun sortUnspents(unspents: List<UnspentOutput>): List<UnspentOutput> {
        Collections.sort(unspents, Bip69.inputComparator)
        return unspents
    }
}

class ShuffleSorter : ITransactionDataSorter {
    override fun sortOutputs(outputs: List<TransactionOutput>): List<TransactionOutput> {
        return outputs.shuffled()
    }

    override fun sortUnspents(unspents: List<UnspentOutput>): List<UnspentOutput> {
        return unspents.shuffled()
    }
}

class StraightSorter : ITransactionDataSorter {
    override fun sortOutputs(outputs: List<TransactionOutput>): List<TransactionOutput> {
        return outputs
    }

    override fun sortUnspents(unspents: List<UnspentOutput>): List<UnspentOutput> {
        return unspents
    }
}
