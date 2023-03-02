package com.lyfebloc.dashkit.masternodelist

import com.lyfebloc.bitcoincore.core.IHasher
import com.lyfebloc.bitcoincore.utils.HashUtils
import com.lyfebloc.dashkit.IMerkleHasher

class MerkleRootHasher: IHasher, IMerkleHasher {

    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }

    override fun hash(first: ByteArray, second: ByteArray): ByteArray {
        return HashUtils.doubleSha256(first + second)
    }
}
