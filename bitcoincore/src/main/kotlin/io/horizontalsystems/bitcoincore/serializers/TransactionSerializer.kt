package com.lyfebloc.bitcoincore.serializers

import com.lyfebloc.bitcoincore.io.BitcoinInputMarkable
import com.lyfebloc.bitcoincore.io.BitcoinOutput
import com.lyfebloc.bitcoincore.models.Transaction
import com.lyfebloc.bitcoincore.models.TransactionInput
import com.lyfebloc.bitcoincore.models.TransactionOutput
import com.lyfebloc.bitcoincore.storage.FullTransaction
import com.lyfebloc.bitcoincore.storage.InputToSign
import com.lyfebloc.bitcoincore.transactions.scripts.OpCodes
import com.lyfebloc.bitcoincore.transactions.scripts.ScriptType
import com.lyfebloc.bitcoincore.utils.HashUtils

object TransactionSerializer {
    fun deserialize(input: BitcoinInputMarkable): FullTransaction {
        val transaction = Transaction()
        val inputs = mutableListOf<TransactionInput>()
        val outputs = mutableListOf<TransactionOutput>()

        transaction.version = input.readInt()
        input.mark()
        val marker = 0xff and input.readUnsignedByte()
        val inputCount = if (marker == 0) {  // segwit marker: 0x00
            input.read()  // skip segwit flag: 0x01
            transaction.segwit = true
            input.readVarInt()
        } else {
            input.reset()
            input.readVarInt()
        }

        //  inputs
        for (i in 0 until inputCount) {
            inputs.add(InputSerializer.deserialize(input))
        }

        //  outputs
        val outputCount = input.readVarInt()
        for (i in 0 until outputCount) {
            outputs.add(OutputSerializer.deserialize(input, i))
        }

        //  extract witness data
        if (transaction.segwit) {
            inputs.forEach {
                it.witness = InputSerializer.deserializeWitness(input)
            }
        }

        transaction.lockTime = input.readUnsignedInt()

        return FullTransaction(transaction, inputs, outputs)
    }

    fun serialize(transaction: FullTransaction, withWitness: Boolean = true): ByteArray {
        val header = transaction.header
        val buffer = BitcoinOutput()
        buffer.writeInt(header.version)

        if (header.segwit && withWitness) {
            buffer.writeByte(0) // marker 0x00
            buffer.writeByte(1) // flag 0x01
        }

        // inputs
        buffer.writeVarInt(transaction.inputs.size.toLong())
        transaction.inputs.forEach { buffer.write(InputSerializer.serialize(it)) }

        // outputs
        buffer.writeVarInt(transaction.outputs.size.toLong())
        transaction.outputs.forEach { buffer.write(OutputSerializer.serialize(it)) }

        //  serialize witness data
        if (header.segwit && withWitness) {
            transaction.inputs.forEach { buffer.write(InputSerializer.serializeWitness(it.witness)) }
        }

        buffer.writeUnsignedInt(header.lockTime)
        return buffer.toByteArray()
    }

    fun serializeForSignature(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, inputIndex: Int, isWitness: Boolean = false): ByteArray {
        val buffer = BitcoinOutput().writeInt(transaction.version)
        if (isWitness) {
            val outpoints = BitcoinOutput()
            val sequences = BitcoinOutput()

            for (inputToSign in inputsToSign) {
                outpoints.write(InputSerializer.serializeOutpoint(inputToSign))
                sequences.writeInt32(inputToSign.input.sequence)
            }

            buffer.write(HashUtils.doubleSha256(outpoints.toByteArray())) // hash prevouts
            buffer.write(HashUtils.doubleSha256(sequences.toByteArray())) // hash sequence

            val inputToSign = inputsToSign[inputIndex]
            val previousOutput = checkNotNull(inputToSign.previousOutput) { throw Exception("no previous output") }

            buffer.write(InputSerializer.serializeOutpoint(inputToSign))

            when (previousOutput.scriptType) {
                ScriptType.P2SH -> {
                    val script = previousOutput.redeemScript ?: throw Exception("no previous output script")
                    buffer.writeVarInt(script.size.toLong())
                    buffer.write(script)
                }
                else -> {
                    buffer.write(OpCodes.push(OpCodes.p2pkhStart + OpCodes.push(previousOutput.keyHash!!) + OpCodes.p2pkhEnd))
                }
            }

            buffer.writeLong(previousOutput.value)
            buffer.writeInt32(inputToSign.input.sequence)

            val hashOutputs = BitcoinOutput()
            for (output in outputs) {
                hashOutputs.write(OutputSerializer.serialize(output))
            }

            buffer.write(HashUtils.doubleSha256(hashOutputs.toByteArray()))
        } else {
            // inputs
            buffer.writeVarInt(inputsToSign.size.toLong())
            inputsToSign.forEachIndexed { index, input ->
                buffer.write(InputSerializer.serializeForSignature(input, index == inputIndex))
            }

            // outputs
            buffer.writeVarInt(outputs.size.toLong())
            outputs.forEach { buffer.write(OutputSerializer.serialize(it)) }
        }

        buffer.writeUnsignedInt(transaction.lockTime)
        return buffer.toByteArray()
    }
}
