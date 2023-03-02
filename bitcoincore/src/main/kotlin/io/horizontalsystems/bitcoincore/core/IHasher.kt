package com.lyfebloc.bitcoincore.core

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}
