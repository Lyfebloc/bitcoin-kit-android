package com.lyfebloc.dashkit.models

class InstantTransactionState {
    var instantTransactionHashes = mutableListOf<ByteArray>()

    fun append(hash: ByteArray) {
        instantTransactionHashes.add(hash)
    }
}
