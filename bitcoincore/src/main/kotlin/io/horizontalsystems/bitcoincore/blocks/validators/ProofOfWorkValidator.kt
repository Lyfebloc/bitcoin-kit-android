package com.lyfebloc.bitcoincore.blocks.validators

import com.lyfebloc.bitcoincore.crypto.CompactBits
import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.models.Block
import java.math.BigInteger

class ProofOfWorkValidator : IBlockChainedValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        check(BigInteger(block.headerHash.toReversedHex(), 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProofOfWork()
        }
    }
}
