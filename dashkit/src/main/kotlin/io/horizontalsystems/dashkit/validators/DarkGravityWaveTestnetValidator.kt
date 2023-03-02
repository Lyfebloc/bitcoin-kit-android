package com.lyfebloc.dashkit.validators

import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorException
import com.lyfebloc.bitcoincore.blocks.validators.IBlockChainedValidator
import com.lyfebloc.bitcoincore.crypto.CompactBits
import com.lyfebloc.bitcoincore.models.Block

class DarkGravityWaveTestnetValidator(
        private val targetSpacing: Int,
        private val targetTimespan: Long,
        private val maxTargetBits: Long,
        private val powDGWHeight: Int
) : IBlockChainedValidator {

    override fun validate(block: Block, previousBlock: Block) {
        if (block.timestamp > previousBlock.timestamp + 2 * targetTimespan) { // more than 2 cycles
            if (block.bits != maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val blockTarget = CompactBits.decode(previousBlock.bits)

        var expectedBits = CompactBits.encode(blockTarget.multiply(10.toBigInteger()))
        if (expectedBits > maxTargetBits) {
            expectedBits = maxTargetBits
        }

        if (expectedBits != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height >= powDGWHeight && block.timestamp > previousBlock.timestamp + 4 * targetSpacing
    }
}
