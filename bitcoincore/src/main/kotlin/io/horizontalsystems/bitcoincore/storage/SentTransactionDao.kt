package com.lyfebloc.bitcoincore.storage

import androidx.room.*
import com.lyfebloc.bitcoincore.models.SentTransaction

@Dao
interface SentTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: SentTransaction)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: SentTransaction)

    @Query("select * from SentTransaction where hash = :hash limit 1")
    fun getTransaction(hash: ByteArray): SentTransaction?

    @Delete
    fun delete(transaction: SentTransaction)
}
