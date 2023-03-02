package com.lyfebloc.bitcoincore.blocks

import com.lyfebloc.bitcoincore.BitcoinCore
import com.lyfebloc.bitcoincore.core.IBlockSyncListener
import com.lyfebloc.bitcoincore.core.IPublicKeyManager
import com.lyfebloc.bitcoincore.core.IStorage
import com.lyfebloc.bitcoincore.managers.BloomFilterManager
import com.lyfebloc.bitcoincore.managers.PublicKeyManager
import com.lyfebloc.bitcoincore.models.BlockHash
import com.lyfebloc.bitcoincore.models.Checkpoint
import com.lyfebloc.bitcoincore.models.MerkleBlock
import com.lyfebloc.bitcoincore.network.Network
import com.lyfebloc.bitcoincore.transactions.BlockTransactionProcessor

class BlockSyncer(
        private val storage: IStorage,
        private val blockchain: Blockchain,
        private val transactionProcessor: BlockTransactionProcessor,
        private val publicKeyManager: IPublicKeyManager,
        private val checkpoint: Checkpoint,
        private val state: State = State()
) {

    var listener: IBlockSyncListener? = null

    private val sqliteMaxVariableNumber = 999

    val localDownloadedBestBlockHeight: Int
        get() = storage.lastBlock()?.height ?: 0

    val localKnownBestBlockHeight: Int
        get() {
            val blockHashes = storage.getBlockchainBlockHashes()
            val headerHashes = blockHashes.map { it.headerHash }
            val existingBlocksCount = headerHashes.chunked(sqliteMaxVariableNumber).map {
                storage.blocksCount(it)
            }.sum()

            return localDownloadedBestBlockHeight.plus(blockHashes.size - existingBlocksCount)
        }

    fun prepareForDownload() {
        handlePartialBlocks()

        clearPartialBlocks()
        clearBlockHashes() // we need to clear block hashes when "syncPeer" is disconnected

        blockchain.handleFork()
    }

    fun downloadStarted() {
    }

    fun downloadIterationCompleted() {
        if (state.iterationHasPartialBlocks) {
            handlePartialBlocks()
        }
    }

    fun downloadCompleted() {
        blockchain.handleFork()
    }

    fun downloadFailed() {
        prepareForDownload()
    }

    fun getBlockHashes(): List<BlockHash> {
        return storage.getBlockHashesSortedBySequenceAndHeight(limit = 500)
    }

    fun getBlockLocatorHashes(peerLastBlockHeight: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()

        storage.getLastBlockchainBlockHash()?.headerHash?.let {
            result.add(it)
        }

        if (result.isEmpty()) {
            storage.getBlocks(heightGreaterThan = checkpoint.block.height, sortedBy = "height", limit = 10).forEach {
                result.add(it.headerHash)
            }
        }

        val lastBlock = storage.getBlock(peerLastBlockHeight)
        if (lastBlock == null) {
            result.add(checkpoint.block.headerHash)
        } else if (!result.contains(lastBlock.headerHash)) {
            result.add(lastBlock.headerHash)
        }

        return result
    }

    fun addBlockHashes(blockHashes: List<ByteArray>) {
        var lastSequence = storage.getLastBlockHash()?.sequence ?: 0

        val existingHashes = storage.getBlockHashHeaderHashes()
        val newBlockHashes = blockHashes.filter { existingHashes.none { n -> n.contentEquals(it) } }.map {
            BlockHash(it, 0, ++lastSequence)
        }

        storage.addBlockHashes(newBlockHashes)
    }

    fun handleMerkleBlock(merkleBlock: MerkleBlock, maxBlockHeight: Int) {
        val height = merkleBlock.height

        val block = when (height) {
            null -> blockchain.connect(merkleBlock)
            else -> blockchain.forceAdd(merkleBlock, height)
        }

        try {
            transactionProcessor.processReceived(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks)
        } catch (e: BloomFilterManager.BloomFilterExpired) {
            state.iterationHasPartialBlocks = true
        }

        if (state.iterationHasPartialBlocks) {
            storage.setBlockPartial(block.headerHash)
        } else {
            storage.deleteBlockHash(block.headerHash)
        }

        listener?.onCurrentBestBlockHeightUpdate(block.height, maxBlockHeight)
    }

    fun shouldRequest(blockHash: ByteArray): Boolean {
        return storage.getBlock(blockHash) == null
    }

    private fun clearPartialBlocks() {
        val excludedHashes = listOf(checkpoint.block.headerHash) + checkpoint.additionalBlocks.map { it.headerHash }
        val toDelete = storage.getBlockHashHeaderHashes(except = excludedHashes)

        toDelete.chunked(sqliteMaxVariableNumber).forEach {
            val blocksToDelete = storage.getBlocks(hashes = it)
            val partialBlocksToDelete = blocksToDelete.filter { block -> block.partial }

            blockchain.deleteBlocks(partialBlocksToDelete)
        }
    }

    private fun handlePartialBlocks() {
        publicKeyManager.fillGap()
        state.iterationHasPartialBlocks = false
    }

    private fun clearBlockHashes() {
        storage.deleteBlockchainBlockHashes()
    }

    class State(var iterationHasPartialBlocks: Boolean = false)

    companion object {
        fun resolveCheckpoint(syncMode: BitcoinCore.SyncMode, network: Network, storage: IStorage): Checkpoint {
            val lastBlock = storage.lastBlock()

            val checkpoint = if (syncMode is BitcoinCore.SyncMode.Full) {
                network.bip44Checkpoint
            } else {
                val lastCheckpoint = network.lastCheckpoint
                if (lastBlock != null && lastBlock.height < lastCheckpoint.block.height) {
                    // during app updating there may be case when the last block in DB is earlier than new checkpoint block
                    // in this case we set the very first checkpoint block for bip44,
                    // since it surely will be earlier than the last block in DB
                    network.bip44Checkpoint
                } else {
                    lastCheckpoint
                }
            }

            if (lastBlock == null) {
                storage.saveBlock(checkpoint.block)
                checkpoint.additionalBlocks.forEach { block ->
                    storage.saveBlock(block)
                }
            }

            return checkpoint
        }
    }

}
