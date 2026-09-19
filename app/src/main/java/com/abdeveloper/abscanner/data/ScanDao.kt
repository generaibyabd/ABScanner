package com.abdeveloper.abscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scans WHERE isSaved = 1 ORDER BY timestamp DESC")
    fun getSavedItems(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scans WHERE title LIKE '%' || :query || '%' OR rawValue LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<ScanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScanItem): Long

    @Query("UPDATE scans SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSaved(id: Long, isSaved: Boolean)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans WHERE isSaved = 0")
    suspend fun clearHistoryOnly()

    @Query("DELETE FROM scans")
    suspend fun clearAll()

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ScanItem?
}
