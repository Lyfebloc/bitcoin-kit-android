package com.lyfebloc.dashkit.instantsend

import com.lyfebloc.dashkit.models.InstantTransactionInput
import java.util.*

class InstantSendFactory {

    fun instantTransactionInput(txHash: ByteArray, inputTxHash: ByteArray, voteCount: Int, blockHeight: Int?) : InstantTransactionInput {
        val timeCreated = Date().time / 1000

        return InstantTransactionInput(txHash, inputTxHash, timeCreated, voteCount, blockHeight)
    }

}
