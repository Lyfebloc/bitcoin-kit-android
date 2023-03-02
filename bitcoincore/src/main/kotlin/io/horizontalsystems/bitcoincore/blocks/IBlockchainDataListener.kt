package com.lyfebloc.bitcoincore.blocks

import com.lyfebloc.bitcoincore.models.Block
import com.lyfebloc.bitcoincore.models.Transaction

interface IBlockchainDataListener {
    fun onBlockInsert(block: Block)
    fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?)
    fun onTransactionsDelete(hashes: List<String>)
}
