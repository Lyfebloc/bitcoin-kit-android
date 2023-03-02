package com.lyfebloc.litecoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.lyfebloc.bitcoincore.AbstractKit
import com.lyfebloc.bitcoincore.BitcoinCore
import com.lyfebloc.bitcoincore.BitcoinCore.SyncMode
import com.lyfebloc.bitcoincore.BitcoinCoreBuilder
import com.lyfebloc.bitcoincore.blocks.validators.BitsValidator
import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorChain
import com.lyfebloc.bitcoincore.blocks.validators.BlockValidatorSet
import com.lyfebloc.bitcoincore.blocks.validators.LegacyTestNetDifficultyValidator
import com.lyfebloc.bitcoincore.managers.*
import com.lyfebloc.bitcoincore.network.Network
import com.lyfebloc.bitcoincore.storage.CoreDatabase
import com.lyfebloc.bitcoincore.storage.Storage
import com.lyfebloc.bitcoincore.utils.Base58AddressConverter
import com.lyfebloc.bitcoincore.utils.PaymentAddressParser
import com.lyfebloc.bitcoincore.utils.SegwitAddressConverter
import com.lyfebloc.hdwalletkit.HDExtendedKey
import com.lyfebloc.hdwalletkit.HDWallet.Purpose
import com.lyfebloc.hdwalletkit.Mnemonic
import com.lyfebloc.litecoinkit.validators.LegacyDifficultyAdjustmentValidator
import com.lyfebloc.litecoinkit.validators.ProofOfWorkValidator

class LitecoinKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    constructor(
        context: Context,
        words: List<String>,
        passphrase: String,
        walletId: String,
        networkType: NetworkType = NetworkType.MainNet,
        peerSize: Int = 10,
        syncMode: SyncMode = SyncMode.Api(),
        confirmationsThreshold: Int = 6,
        purpose: Purpose = Purpose.BIP44
    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, peerSize, syncMode, confirmationsThreshold, purpose)

    constructor(
        context: Context,
        seed: ByteArray,
        walletId: String,
        networkType: NetworkType = NetworkType.MainNet,
        peerSize: Int = 10,
        syncMode: SyncMode = SyncMode.Api(),
        confirmationsThreshold: Int = 6,
        purpose: Purpose = Purpose.BIP44
    ) : this(context, HDExtendedKey(seed, purpose), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context
     * @param extendedKey HDExtendedKey that contains HDKey and version
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     */
    constructor(
        context: Context,
        extendedKey: HDExtendedKey,
        walletId: String,
        networkType: NetworkType = NetworkType.MainNet,
        peerSize: Int = 10,
        syncMode: SyncMode = SyncMode.Api(),
        confirmationsThreshold: Int = 6
    ) {
        val purpose = extendedKey.info.purpose
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, purpose))
        val storage = Storage(database)
        var initialSyncUrl = ""

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://ltc.lyfebloc.xyz/api"
                MainNetLitecoin()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = ""
                TestNetLitecoin()
            }
        }

        val paymentAddressParser = PaymentAddressParser("litecoin", removeScheme = true)
        val initialSyncApi = BCoinApi(initialSyncUrl)

        val blockValidatorSet = BlockValidatorSet()

        val proofOfWorkValidator = ProofOfWorkValidator(ScryptHasher())
        blockValidatorSet.addBlockValidator(proofOfWorkValidator)

        val blockValidatorChain = BlockValidatorChain()

        val blockHelper = BlockValidatorHelper(storage)

        if (networkType == NetworkType.MainNet) {
            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            blockValidatorChain.add(BitsValidator())
        } else if (networkType == NetworkType.TestNet) {
            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            blockValidatorChain.add(LegacyTestNetDifficultyValidator(storage, heightInterval, targetSpacing, maxTargetBits))
            blockValidatorChain.add(BitsValidator())
        }

        blockValidatorSet.addBlockValidator(blockValidatorChain)

        val coreBuilder = BitcoinCoreBuilder()

        bitcoinCore = coreBuilder
            .setContext(context)
            .setExtendedKey(extendedKey)
            .setNetwork(network)
            .setPaymentAddressParser(paymentAddressParser)
            .setPeerSize(peerSize)
            .setSyncMode(syncMode)
            .setConfirmationThreshold(confirmationsThreshold)
            .setStorage(storage)
            .setInitialSyncApi(initialSyncApi)
            .setBlockValidator(blockValidatorSet)
            .build()

        //  extending bitcoinCore

        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        when (purpose) {
            Purpose.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
            }
            Purpose.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }
            Purpose.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(KeyHashRestoreKeyConverter())
            }
        }
    }

    companion object {

        const val maxTargetBits: Long = 0x1e0fffff      // Maximum difficulty
        const val targetSpacing = 150                   // 2.5 minutes per block.
        const val targetTimespan: Long = 302400         // 3.5 days per difficulty cycle, on average.
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, purpose: Purpose): String =
            "Litecoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${purpose.name}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                for (purpose in Purpose.values())
                    try {
                        SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, purpose)))
                    } catch (ex: Exception) {
                        continue
                    }
            }
        }
    }

}
