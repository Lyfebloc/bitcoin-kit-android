package com.lyfebloc.dashkit.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.lyfebloc.bitcoincore.models.*

class DashTransactionInfo : TransactionInfo {

    var instantTx: Boolean = false

    constructor(uid: String,
                transactionHash: String,
                transactionIndex: Int,
                inputs: List<TransactionInputInfo>,
                outputs: List<TransactionOutputInfo>,
                amount: Long,
                type: TransactionType,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus,
                conflictingTxHash: String?,
                instantTx: Boolean
    ) : super(uid, transactionHash, transactionIndex, inputs, outputs, amount, type, fee, blockHeight, timestamp, status, conflictingTxHash) {
        this.instantTx = instantTx
    }

    @Throws
    constructor(serialized: String) : super(serialized) {
        val jsonObject = Json.parse(serialized).asObject()
        this.instantTx = jsonObject["instantTx"].asBoolean()
    }

    override fun asJsonObject(): JsonObject {
        val jsonObject = super.asJsonObject()
        jsonObject["instantTx"] = instantTx
        return jsonObject
    }

}
