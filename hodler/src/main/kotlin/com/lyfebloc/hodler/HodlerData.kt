package com.lyfebloc.hodler

import com.lyfebloc.bitcoincore.core.IPluginData

data class HodlerData(val lockTimeInterval: LockTimeInterval) : IPluginData
