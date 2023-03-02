package com.lyfebloc.bitcoincore.storage

import androidx.room.TypeConverter
import com.lyfebloc.bitcoincore.extensions.hexToByteArray
import com.lyfebloc.bitcoincore.extensions.toHexString

class WitnessConverter {

    @TypeConverter
    fun fromWitness(list: List<ByteArray>): String {
        return list.joinToString(", ") {
            it.toHexString()
        }
    }

    @TypeConverter
    fun toWitness(data: String): List<ByteArray> = when {
        data.isEmpty() -> listOf()
        else -> data.split(", ").map {
            it.hexToByteArray()
        }
    }
}
