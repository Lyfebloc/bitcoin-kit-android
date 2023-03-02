package com.lyfebloc.dashkit.core

import com.lyfebloc.bitcoincore.core.IHasher
import com.lyfebloc.bitcoincore.utils.HashUtils

class SingleSha256Hasher : IHasher {
    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.sha256(data)
    }
}
