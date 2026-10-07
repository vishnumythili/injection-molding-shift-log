package com.shiftlog.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
private val dayFmt = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.getDefault())

fun fmtDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFmt)

fun fmtDay(epochDay: Long): String {
    val today = LocalDate.now().toEpochDay()
    return when (epochDay) {
        today -> "Today"
        today - 1 -> "Yesterday"
        else -> LocalDate.ofEpochDay(epochDay).format(dayFmt)
    }
}

/** A: 06-14, B: 14-22, C: 22-06 */
fun currentShift(): String {
    val h = LocalTime.now().hour
    return when (h) {
        in 6..13 -> "A"
        in 14..21 -> "B"
        else -> "C"
    }
}

val shiftHours = mapOf("A" to "06–14", "B" to "14–22", "C" to "22–06")

fun pct(v: Double): String = String.format(Locale.US, "%.1f%%", v)

/** Green under 2 %, amber under 5 %, red above. */
fun rejColor(p: Double): Color = when {
    p < 2.0 -> Color(0xFF2E7D32)
    p < 5.0 -> Color(0xFFE08A00)
    else -> Color(0xFFC62828)
}

private fun csv(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

fun exportCsv(ctx: Context, list: List<ShiftEntry>) {
    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
    val f = File(dir, "shift_production_${LocalDate.now()}.csv")
    val sb = StringBuilder("﻿") // BOM so Excel opens UTF-8 correctly
    sb.append(
        (listOf("Date", "Shift", "Machine", "Product", "Target", "Produced", "OK", "Rejected", "Rej %") +
            Rej.reasons + listOf("Downtime min", "Downtime reason", "Operator", "Remarks"))
            .joinToString(",")
    ).append("\n")
    list.sortedWith(compareBy({ it.date }, { it.shift })).forEach { e ->
        val m = e.rejMap
        sb.append(
            (listOf(
                LocalDate.ofEpochDay(e.date).toString(), e.shift, csv(e.machine), csv(e.product),
                e.target.toString(), e.produced.toString(), e.ok.toString(), e.rejected.toString(),
                String.format(Locale.US, "%.2f", e.rejPct)
            ) + Rej.reasons.map { (m[it] ?: 0).toString() } +
                listOf(e.downtimeMin.toString(), csv(e.downtimeReason), csv(e.operator), csv(e.remarks)))
                .joinToString(",")
        ).append("\n")
    }
    f.writeText(sb.toString())
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Export production data"))
}
