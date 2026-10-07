package com.shiftlog.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Rejection reasons shown in the entry form. Edit this list to suit your plant. */
object Rej {
    val reasons = listOf(
        "Flash", "Short shot", "Sink mark", "Burn mark", "Black spots",
        "Flow marks", "Warpage", "Dimension", "Other"
    )

    val downtimeReasons = listOf(
        "None", "Mold change", "Material shortage", "Machine breakdown",
        "Mold maintenance", "Power cut", "Quality hold", "No manpower", "Other"
    )

    fun parse(s: String): Map<String, Int> =
        if (s.isBlank()) emptyMap()
        else s.split("|").mapNotNull {
            val p = it.split("=")
            val n = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && n != null && n > 0) p[0] to n else null
        }.toMap()

    fun encode(m: Map<String, Int>): String =
        m.filterValues { it > 0 }.entries.joinToString("|") { "${it.key}=${it.value}" }
}

@Entity(tableName = "entries")
data class ShiftEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                 // LocalDate epoch day
    val shift: String,              // A / B / C
    val machine: String,
    val product: String,
    val target: Int,
    val produced: Int,              // total pieces made (OK + rejected)
    val rejections: String,         // "Flash=3|Short shot=2"
    val downtimeMin: Int,
    val downtimeReason: String,
    val operator: String,
    val remarks: String
) {
    val rejMap: Map<String, Int> get() = Rej.parse(rejections)
    val rejected: Int get() = rejMap.values.sum()
    val ok: Int get() = produced - rejected
    val rejPct: Double get() = if (produced == 0) 0.0 else rejected * 100.0 / produced
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date DESC, shift DESC, id DESC")
    fun all(): Flow<List<ShiftEntry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): ShiftEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ShiftEntry)

    @Delete
    suspend fun delete(e: ShiftEntry)
}

@Database(entities = [ShiftEntry::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): EntryDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "shiftlog.db")
                .build().also { inst = it }
        }
    }
}
