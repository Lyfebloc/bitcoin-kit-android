package com.lyfebloc.bitcoincore.blocks

import com.lyfebloc.bitcoincore.network.peer.Peer

interface IPeerSyncListener {
    fun onAllPeersSynced() = Unit
    fun onPeerSynced(peer: Peer) = Unit
}
