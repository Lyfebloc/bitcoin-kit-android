package com.lyfebloc.bitcoincore.blocks

import com.lyfebloc.bitcoincore.crypto.BloomFilter
import com.lyfebloc.bitcoincore.managers.BloomFilterManager
import com.lyfebloc.bitcoincore.network.peer.Peer
import com.lyfebloc.bitcoincore.network.peer.PeerGroup
import com.lyfebloc.bitcoincore.network.peer.PeerManager

class BloomFilterLoader(private val bloomFilterManager: BloomFilterManager, private val peerManager: PeerManager)
    : PeerGroup.Listener, BloomFilterManager.Listener {

    override fun onPeerConnect(peer: Peer) {
        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }
    }

    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peerManager.connected().forEach {
            it.filterLoad(bloomFilter)
        }
    }
}
