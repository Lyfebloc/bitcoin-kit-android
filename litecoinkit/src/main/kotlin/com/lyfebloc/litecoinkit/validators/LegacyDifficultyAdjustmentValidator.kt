package com.lyfebloc.litecoinkit.validators

import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorException
import com.lyfebloc.bitcoincore.blocks.validators.IBlockChainedValidator
import com.lyfebloc.bitcoincore.crypto.CompactBits
import com.lyfebloc.bitcoincore.managers.BlockValidatorHelper
import com.lyfebloc.bitcoincore.models.Block
import java.math.BigInteger
import kotlin.math.min

class LegacyDifficultyAdjustmentValidator(
        private val validatorHelper: BlockValidatorHelper,
        private val heightInterval: Long,
        private val targetTimespan: Long,
        private val maxTargetBits: Long
) : IBlockChainedValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height % heightInterval == 0L
    }

    override fun validate(block: Block, previousBlock: Block) {
        val beforeLastCheckPointBlock = checkNotNull(validatorHelper.getPrevious(block, heightInterval.toInt() + 1)) {
            BlockValidatorException.NoCheckpointBlock()
        }

        //  Limit the adjustment step
        var timespan = previousBlock.timestamp - beforeLastCheckPointBlock.timestamp
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4

        var newTarget = CompactBits.decode(previousBlock.bits)
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan))
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan))

        //  Difficulty hit proof of work limit: newTarget.toString(16)
        val newDifficulty = min(CompactBits.encode(newTarget), maxTargetBits)

        if (newDifficulty != block.bits) {
            throw BlockValidatorException.NotDifficultyTransitionEqualBits()
        }
    }

}
