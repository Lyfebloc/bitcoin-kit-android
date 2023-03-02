package com.lyfebloc.bitcoincore.blocks

import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorException
import com.lyfebloc.bitcoincore.blocks.validators.IBlockValidator
import com.lyfebloc.bitcoincore.core.IStorage
import com.lyfebloc.bitcoincore.extensions.toReversedHex
import com.lyfebloc.bitcoincore.models.Block
import com.lyfebloc.bitcoincore.models.MerkleBlock

class Blockchain(
        private val storage: IStorage,
        private val blockValidator: IBlockValidator?,
        private val dataListener: IBlockchainDataListener
) {

    fun connect(merkleBlock: MerkleBlock): Block {
        val blockInDB = storage.getBlock(merkleBlock.blockHash)
        if (blockInDB != null) {
            return blockInDB
        }

        val parentBlock = storage.getBlock(merkleBlock.header.previousBlockHeaderHash)
                ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        blockValidator?.validate(block, parentBlock)

        block.stale = true

        if (block.height % 2016 == 0) {
            storage.deleteBlocksWithoutTransactions(block.height - 2016)
        }

        return addBlockAndNotify(block)
    }

    fun forceAdd(merkleBlock: MerkleBlock, height: Int): Block {
        val blockInDB = storage.getBlock(merkleBlock.blockHash)
        if (blockInDB != null) {
            return blockInDB
        }

        return addBlockAndNotify(Block(merkleBlock.header, height))
    }

    fun handleFork() {
        val firstStaleHeight = storage.getBlock(stale = true, sortedHeight = "ASC")
                ?.height ?: return

        val lastNotStaleHeight = storage.getBlock(stale = false, sortedHeight = "DESC")
                ?.height ?: 0

        if (firstStaleHeight <= lastNotStaleHeight) {
            val lastStaleHeight = storage.getBlock(stale = true, sortedHeight = "DESC")?.height
                    ?: firstStaleHeight

            if (lastStaleHeight > lastNotStaleHeight) {
                val notStaleBlocks = storage.getBlocks(heightGreaterOrEqualTo = firstStaleHeight, stale = false)
                deleteBlocks(notStaleBlocks)
                storage.unstaleAllBlocks()
            } else {
                val staleBlocks = storage.getBlocks(stale = true)
                deleteBlocks(staleBlocks)
            }
        } else {
            storage.unstaleAllBlocks()
        }
    }

    fun deleteBlocks(blocksToDelete: List<Block>) {
        val deletedTransactionIds = mutableListOf<String>()

        blocksToDelete.forEach { block ->
            deletedTransactionIds.addAll(storage.getBlockTransactions(block).map { it.hash.toReversedHex() })
        }

        storage.deleteBlocks(blocksToDelete)

        dataListener.onTransactionsDelete(deletedTransactionIds)
    }

    private fun addBlockAndNotify(block: Block): Block {
        storage.addBlock(block)
        dataListener.onBlockInsert(block)

        return block
    }
}
