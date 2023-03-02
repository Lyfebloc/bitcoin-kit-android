package com.lyfebloc.bitcoincore.core

import com.lyfebloc.bitcoincore.models.TransactionDataSortType
import com.lyfebloc.bitcoincore.utils.Bip69Sorter
import com.lyfebloc.bitcoincore.utils.ShuffleSorter
import com.lyfebloc.bitcoincore.utils.StraightSorter

class TransactionDataSorterFactory : ITransactionDataSorterFactory {
    override fun sorter(type: TransactionDataSortType): ITransactionDataSorter {
        return when (type) {
            TransactionDataSortType.None -> StraightSorter()
            TransactionDataSortType.Shuffle -> ShuffleSorter()
            TransactionDataSortType.Bip69 -> Bip69Sorter()
        }
    }
}
