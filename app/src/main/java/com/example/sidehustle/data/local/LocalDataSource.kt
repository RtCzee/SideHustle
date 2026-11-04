package com.example.sidehustle.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** A per-user JSON cache keeps API DTOs intact while giving all core lists durable storage. */
@Entity(tableName = "cached_records", primaryKeys = ["userId", "kind", "id"])
data class CachedRecord(val userId: String, val kind: String, val id: String, val json: String)

@Entity(tableName = "sync_queue")
data class QueuedWrite(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    @ColumnInfo(index = true, unique = true) val operationId: String = UUID.randomUUID().toString(),
    val userId: String,
    val kind: String,
    val action: String,
    val recordId: String,
    val payload: String,
    val changedAt: Long = System.currentTimeMillis(),
)

@Dao
interface LocalDao {
    @Query("SELECT json FROM cached_records WHERE userId = :userId AND kind = :kind") suspend fun list(userId: String, kind: String): List<String>
    @Query("SELECT json FROM cached_records WHERE userId = :userId AND kind = :kind AND id = :id LIMIT 1") suspend fun get(userId: String, kind: String, id: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(record: CachedRecord)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAll(records: List<CachedRecord>)
    @Query("DELETE FROM cached_records WHERE userId = :userId AND kind = :kind") suspend fun clear(userId: String, kind: String)
    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY sequence") suspend fun pending(userId: String): List<QueuedWrite>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueue(write: QueuedWrite)
    @Query("DELETE FROM sync_queue WHERE operationId = :operationId") suspend fun remove(operationId: String)
    @Query("DELETE FROM cached_records WHERE userId = :userId") suspend fun clearUser(userId: String)
    @Query("DELETE FROM sync_queue WHERE userId = :userId") suspend fun clearQueue(userId: String)
}

@Database(entities = [CachedRecord::class, QueuedWrite::class], version = 1, exportSchema = false)
abstract class SideHustleDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao
    companion object {
        @Volatile private var instance: SideHustleDatabase? = null
        fun get(context: Context): SideHustleDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SideHustleDatabase::class.java, "sidehustle-offline.db").build().also { instance = it }
        }
    }
}

class LocalDataSource(internal val dao: LocalDao, internal val gson: Gson = Gson()) {
    internal suspend inline fun <reified T> readAll(user: String, kind: String): List<T> = dao.list(user, kind).mapNotNull { runCatching { gson.fromJson(it, T::class.java) }.getOrNull() }
    internal suspend inline fun <reified T> read(user: String, kind: String, id: String): T? = dao.get(user, kind, id)?.let { runCatching { gson.fromJson(it, T::class.java) }.getOrNull() }
    suspend fun <T> replace(user: String, kind: String, records: List<T>, id: (T) -> String) {
        val pendingIds = dao.pending(user).filter { it.kind == kind }.map { it.recordId }
        val pendingRecords = pendingIds.mapNotNull { recordId -> dao.get(user, kind, recordId)?.let { CachedRecord(user, kind, recordId, it) } }
        dao.clear(user, kind)
        dao.putAll(records.map { CachedRecord(user, kind, id(it), gson.toJson(it)) })
        // Pending local edits survive a refresh until accepted by the server.
        dao.putAll(pendingRecords)
    }
    suspend fun <T> save(user: String, kind: String, id: String, value: T) = dao.put(CachedRecord(user, kind, id, gson.toJson(value)))
    suspend fun enqueue(user: String, kind: String, action: String, id: String, payload: Any) = dao.enqueue(QueuedWrite(userId = user, kind = kind, action = action, recordId = id, payload = gson.toJson(payload)))
    suspend fun pending(user: String) = dao.pending(user)
    suspend fun remove(operationId: String) = dao.remove(operationId)
    suspend fun clearUser(user: String) { dao.clearUser(user); dao.clearQueue(user) }
}
