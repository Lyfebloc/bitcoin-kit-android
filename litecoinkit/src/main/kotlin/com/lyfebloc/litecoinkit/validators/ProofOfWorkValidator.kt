package com.lyfebloc.litecoinkit.validators

import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorException
import com.lyfebloc.bitcoincore.blocks.validators.IBlockChainedValidator
import com.lyfebloc.bitcoincore.crypto.CompactBits
import com.lyfebloc.bitcoincore.extensions.toHexString
import com.lyfebloc.bitcoincore.io.BitcoinOutput
import com.lyfebloc.bitcoincore.models.Block
import com.lyfebloc.litecoinkit.ScryptHasher
import java.math.BigInteger

class ProofOfWorkValidator(private val scryptHasher: ScryptHasher) : IBlockChainedValidator {

    override fun validate(block: Block, previousBlock: Block) {
        val blockHeaderData = getSerializedBlockHeader(block)

        val powHash = scryptHasher.hash(blockHeaderData).toHexString()

        check(BigInteger(powHash, 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProofOfWork()
        }
    }

    private fun getSerializedBlockHeader(block: Block): ByteArray {
        return BitcoinOutput()
                .writeInt(block.version)
                .write(block.previousBlockHash)
                .write(block.merkleRoot)
                .writeUnsignedInt(block.timestamp)
                .writeUnsignedInt(block.bits)
                .writeUnsignedInt(block.nonce)
                .toByteArray()
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

}
