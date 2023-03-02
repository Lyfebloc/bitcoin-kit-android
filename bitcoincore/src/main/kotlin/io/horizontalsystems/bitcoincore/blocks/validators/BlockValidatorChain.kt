package com.lyfebloc.bitcoincore.blocks.validators

import com.lyfebloc.bitcoincore.models.Block

class BlockValidatorChain : IBlockChainedValidator {

    private val concreteValidators = mutableListOf<IBlockChainedValidator>()

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        concreteValidators.firstOrNull { validator ->
            validator.isBlockValidatable(block, previousBlock)
        }?.validate(block, previousBlock)
    }

    fun add(validator: IBlockChainedValidator) {
        concreteValidators.add(validator)
    }
}
