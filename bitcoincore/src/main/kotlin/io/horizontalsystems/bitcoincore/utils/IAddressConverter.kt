package com.lyfebloc.bitcoincore.utils

import com.lyfebloc.bitcoincore.exceptions.AddressFormatException
import com.lyfebloc.bitcoincore.models.Address
import com.lyfebloc.bitcoincore.models.PublicKey
import com.lyfebloc.bitcoincore.transactions.scripts.ScriptType

interface IAddressConverter {
    @Throws
    fun convert(addressString: String): Address

    @Throws
    fun convert(bytes: ByteArray, scriptType: ScriptType = ScriptType.P2PKH): Address

    @Throws
    fun convert(publicKey: PublicKey, scriptType: ScriptType = ScriptType.P2PKH): Address
}

class AddressConverterChain : IAddressConverter {
    private val concreteConverters = mutableListOf<IAddressConverter>()

    fun prependConverter(converter: IAddressConverter) {
        concreteConverters.add(0, converter)
    }

    override fun convert(addressString: String): Address {
        val exceptions = mutableListOf<Exception>()

        for (converter in concreteConverters) {
            try {
                return converter.convert(addressString)
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        val exception = AddressFormatException("No converter in chain could process the address")
        exceptions.forEach {
            exception.addSuppressed(it)
        }

        throw exception
    }

    override fun convert(bytes: ByteArray, scriptType: ScriptType): Address {
        val exceptions = mutableListOf<Exception>()

        for (converter in concreteConverters) {
            try {
                return converter.convert(bytes, scriptType)
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        val exception = AddressFormatException("No converter in chain could process the address")
        exceptions.forEach {
            exception.addSuppressed(it)
        }

        throw exception
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType): Address {
        val exceptions = mutableListOf<Exception>()

        for (converter in concreteConverters) {
            try {
                return converter.convert(publicKey, scriptType)
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        val exception = AddressFormatException("No converter in chain could process the address").also {
            exceptions.forEach { it.addSuppressed(it) }
        }

        throw exception
    }
}
