package com.shiftlog.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private enum class Period(val label: String) {
    TODAY("Today"), WEEK("7 days"), MONTH("30 days"), ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(vm: MainViewModel, onBack: () -> Unit) {
    val all by vm.entries.collectAsState()
    var period by remember { mutableStateOf(Period.WEEK) }
    val ctx = LocalContext.current
    val today = LocalDate.now().toEpochDay()
    val list = remember(all, period) {
        when (period) {
            Period.TODAY -> all.filter { it.date == today }
            Period.WEEK -> all.filter { it.date > today - 7 }
            Period.MONTH -> all.filter { it.date > today - 30 }
            Period.ALL -> all
        }
    }

    val produced = list.sumOf { it.produced }
    val rejected = list.sumOf { it.rejected }
    val target = list.sumOf { it.target }
    val down = list.sumOf { it.downtimeMin }
    val rate = if (produced == 0) 0.0 else rejected * 100.0 / produced

    val byReason = remember(list) {
        val m = mutableMapOf<String, Int>()
        list.forEach { e -> e.rejMap.forEach { (k, v) -> m[k] = (m[k] ?: 0) + v } }
        m.entries.sortedByDescending { it.value }
    }
    val byMachine = remember(list) {
        list.groupBy { it.machine }.map { (m, l) ->
            Triple(m, l.sumOf { it.produced }, l.sumOf { it.rejected })
        }.sortedByDescending { it.second }
    }
    val byDown = remember(list) {
        list.filter { it.downtimeMin > 0 }.groupBy { it.downtimeReason }
            .map { (k, l) -> k to l.sumOf { it.downtimeMin } }.sortedByDescending { it.second }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { exportCsv(ctx, list) }, enabled = list.isNotEmpty()) {
                        Icon(Icons.Default.IosShare, "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = period == p, onClick = { period = p },
                        shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size)
                    ) { Text(p.label) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi("Produced", produced.toString(), Modifier.weight(1f))
                Kpi("Good", (produced - rejected).toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi("Rejection", pct(rate), Modifier.weight(1f), rejColor(rate))
                Kpi(
                    "Target met",
                    if (target > 0) pct((produced - rejected) * 100.0 / target) else "–",
                    Modifier.weight(1f)
                )
            }
            Kpi("Downtime", "${down / 60} h ${down % 60} min", Modifier.fillMaxWidth())

            if (list.isEmpty()) {
                Text(
                    "No entries in this period.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            if (byReason.isNotEmpty()) {
                Block("Rejections by reason") {
                    val max = byReason.first().value.toFloat()
                    byReason.forEach { (k, v) -> BarRow(k, v.toString(), v / max, MaterialTheme.colorScheme.secondary) }
                }
            }
            if (byMachine.isNotEmpty()) {
                Block("By machine") {
                    byMachine.forEach { (m, p, r) ->
                        val rr = if (p == 0) 0.0 else r * 100.0 / p
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(m, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text("$p pcs", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(pct(rr), style = MaterialTheme.typography.titleMedium, color = rejColor(rr))
                        }
                    }
                }
            }
            if (byDown.isNotEmpty()) {
                Block("Downtime by reason") {
                    val max = byDown.first().second.toFloat()
                    byDown.forEach { (k, v) -> BarRow(k, "$v min", v / max, MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium,
                color = color ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun Block(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun BarRow(label: String, value: String, frac: Float, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { frac.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
