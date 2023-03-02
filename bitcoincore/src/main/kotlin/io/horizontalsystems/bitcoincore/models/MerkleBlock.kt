package com.lyfebloc.bitcoincore.models

import com.lyfebloc.bitcoincore.core.HashBytes
import com.lyfebloc.bitcoincore.storage.BlockHeader
import com.lyfebloc.bitcoincore.storage.FullTransaction

class MerkleBlock(val header: BlockHeader, val associatedTransactionHashes: Map<HashBytes, Boolean>) {

    var height: Int? = null
    var associatedTransactions = mutableListOf<FullTransaction>()
    val blockHash = header.hash

    val complete: Boolean
        get() = associatedTransactionHashes.size == associatedTransactions.size

}
