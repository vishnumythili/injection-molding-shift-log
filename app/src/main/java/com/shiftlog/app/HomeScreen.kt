package com.shiftlog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onReports: () -> Unit
) {
    val entries by vm.entries.collectAsState()
    val today = LocalDate.now().toEpochDay()
    val todays = entries.filter { it.date == today }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Shift Log", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onReports) {
                        Icon(Icons.Default.BarChart, contentDescription = "Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New entry") }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { TodayCard(todays) }

            if (entries.isEmpty()) {
                item { EmptyState() }
            } else {
                var last = -1L
                entries.forEach { e ->
                    if (e.date != last) {
                        last = e.date
                        item(key = "h${e.date}") {
                            Text(
                                fmtDay(e.date),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                    item(key = "e${e.id}") { EntryCard(e) { onEdit(e.id) } }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(list: List<ShiftEntry>) {
    val produced = list.sumOf { it.produced }
    val rejected = list.sumOf { it.rejected }
    val rate = if (produced == 0) 0.0 else rejected * 100.0 / produced
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "TODAY", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Produced", produced.toString(), Modifier.weight(1f))
                Stat("Good", (produced - rejected).toString(), Modifier.weight(1f))
                Stat("Rejected", rejected.toString(), Modifier.weight(1f))
                Stat("Rej %", pct(rate), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(
            value, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun EntryCard(e: ShiftEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    e.shift, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.machine, style = MaterialTheme.typography.titleMedium)
                Text(
                    e.product, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                val prog = if (e.target > 0) (e.ok.toFloat() / e.target).coerceIn(0f, 1f) else 0f
                if (e.target > 0) {
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    "${e.ok} good / ${if (e.target > 0) e.target.toString() else "–"} target" +
                        if (e.downtimeMin > 0) "  ·  ${e.downtimeMin} min down" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    pct(e.rejPct), style = MaterialTheme.typography.titleMedium,
                    color = rejColor(e.rejPct)
                )
                Text(
                    "${e.rejected} rej", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Inventory2, null,
            Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text("No entries yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap “New entry” to log your first shift.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
