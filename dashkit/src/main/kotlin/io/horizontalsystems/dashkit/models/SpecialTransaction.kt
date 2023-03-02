package com.lyfebloc.dashkit.models

import com.lyfebloc.bitcoincore.storage.FullTransaction

class SpecialTransaction(
        val transaction: FullTransaction,
        extraPayload: ByteArray,
        forceHashUpdate: Boolean = true
): FullTransaction(transaction.header, transaction.inputs, transaction.outputs, forceHashUpdate)
