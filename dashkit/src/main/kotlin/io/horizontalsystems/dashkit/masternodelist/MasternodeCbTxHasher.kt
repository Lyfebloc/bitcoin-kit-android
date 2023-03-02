package com.lyfebloc.dashkit.masternodelist

import com.lyfebloc.bitcoincore.core.HashBytes
import com.lyfebloc.bitcoincore.core.IHasher
import com.lyfebloc.dashkit.models.CoinbaseTransaction
import com.lyfebloc.dashkit.models.CoinbaseTransactionSerializer

class MasternodeCbTxHasher(private val coinbaseTransactionSerializer: CoinbaseTransactionSerializer, private val hasher: IHasher) {

    fun hash(coinbaseTransaction: CoinbaseTransaction): HashBytes {
        val serialized = coinbaseTransactionSerializer.serialize(coinbaseTransaction)

        return HashBytes(hasher.hash(serialized))
    }

}
