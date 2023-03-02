package com.lyfebloc.bitcoincore.managers

import com.lyfebloc.bitcoincore.models.BlockHash
import com.lyfebloc.bitcoincore.models.PublicKey
import io.reactivex.Single

interface IBlockDiscovery {
    fun discoverBlockHashes(): Single<Pair<List<PublicKey>, List<BlockHash>>>
}
