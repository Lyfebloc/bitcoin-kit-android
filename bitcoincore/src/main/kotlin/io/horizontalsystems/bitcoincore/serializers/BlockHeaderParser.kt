package com.lyfebloc.bitcoincore.serializers

import com.lyfebloc.bitcoincore.core.IHasher
import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.storage.BlockHeader

class BlockHeaderParser(private val hasher: IHasher) {

    fun parse(input: BitcoinInputMarkable): BlockHeader {
        input.mark()
        val payload = input.readBytes(80)
        val hash = hasher.hash(payload)
        input.reset()

        val version = input.readInt()
        val previousBlockHeaderHash = input.readBytes(32)
        val merkleRoot = input.readBytes(32)
        val timestamp = input.readUnsignedInt()
        val bits = input.readUnsignedInt()
        val nonce = input.readUnsignedInt()

        return BlockHeader(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce, hash)
    }
}
