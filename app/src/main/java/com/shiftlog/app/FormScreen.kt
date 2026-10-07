package com.shiftlog.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun FormScreen(vm: MainViewModel, id: Long, onDone: () -> Unit) {
    var ready by remember { mutableStateOf(id <= 0) }
    var existing by remember { mutableStateOf<ShiftEntry?>(null) }
    LaunchedEffect(id) {
        if (id > 0) existing = vm.get(id)
        ready = true
    }
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val all by vm.entries.collectAsState()
    FormBody(
        existing = existing,
        history = all,
        onSave = { vm.save(it); onDone() },
        onDelete = { existing?.let { e -> vm.delete(e) }; onDone() },
        onBack = onDone
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormBody(
    existing: ShiftEntry?,
    history: List<ShiftEntry>,
    onSave: (ShiftEntry) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var date by remember { mutableLongStateOf(existing?.date ?: LocalDate.now().toEpochDay()) }
    var shift by remember { mutableStateOf(existing?.shift ?: currentShift()) }
    var machine by remember { mutableStateOf(existing?.machine ?: "") }
    var product by remember { mutableStateOf(existing?.product ?: "") }
    var target by remember { mutableStateOf(existing?.target?.takeIf { it > 0 }?.toString() ?: "") }
    var produced by remember { mutableStateOf(existing?.produced?.takeIf { it > 0 }?.toString() ?: "") }
    val rej: SnapshotStateMap<String, Int> = remember {
        mutableStateMapOf<String, Int>().also { it.putAll(existing?.rejMap ?: emptyMap()) }
    }
    var downMin by remember { mutableStateOf(existing?.downtimeMin?.takeIf { it > 0 }?.toString() ?: "") }
    var downReason by remember { mutableStateOf(existing?.downtimeReason ?: "None") }
    var operator by remember { mutableStateOf(existing?.operator ?: "") }
    var remarks by remember { mutableStateOf(existing?.remarks ?: "") }

    var showDate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val producedN = produced.toIntOrNull() ?: 0
    val rejectedN = rej.values.sum()
    val okN = producedN - rejectedN
    val rate = if (producedN == 0) 0.0 else rejectedN * 100.0 / producedN

    val machines = remember(history) { history.map { it.machine }.filter { it.isNotBlank() }.distinct().take(8) }
    val products = remember(history) { history.map { it.product }.filter { it.isNotBlank() }.distinct().take(8) }
    val operators = remember(history) { history.map { it.operator }.filter { it.isNotBlank() }.distinct().take(8) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New entry" else "Edit entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(
                        onClick = {
                            error = when {
                                machine.isBlank() -> "Enter the machine"
                                product.isBlank() -> "Enter the product"
                                producedN <= 0 -> "Enter the produced quantity"
                                rejectedN > producedN -> "Rejections exceed produced quantity"
                                else -> null
                            }
                            if (error == null) {
                                onSave(
                                    ShiftEntry(
                                        id = existing?.id ?: 0,
                                        date = date, shift = shift,
                                        machine = machine.trim(), product = product.trim(),
                                        target = target.toIntOrNull() ?: 0,
                                        produced = producedN,
                                        rejections = Rej.encode(rej),
                                        downtimeMin = downMin.toIntOrNull() ?: 0,
                                        downtimeReason = if ((downMin.toIntOrNull() ?: 0) > 0) downReason else "None",
                                        operator = operator.trim(), remarks = remarks.trim()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Save entry", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date + shift
            Section("When") {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(fmtDate(date))
                }
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val keys = shiftHours.keys.toList()
                    keys.forEachIndexed { i, s ->
                        SegmentedButton(
                            selected = shift == s, onClick = { shift = s },
                            shape = SegmentedButtonDefaults.itemShape(i, keys.size)
                        ) { Text("$s  ${shiftHours[s]}") }
                    }
                }
            }

            // What
            Section("Machine & product") {
                OutlinedTextField(
                    machine, { machine = it }, label = { Text("Machine / IMM no.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Suggest(machines) { machine = it }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    product, { product = it }, label = { Text("Product / part name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Suggest(products) { product = it }
            }

            // Output
            Section("Production") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("Target", target, { target = it }, Modifier.weight(1f))
                    NumField("Produced (total)", produced, { produced = it }, Modifier.weight(1f))
                }
            }

            // Rejections
            Section("Rejections") {
                Rej.reasons.forEach { r ->
                    Counter(r, rej[r] ?: 0) { rej[r] = it }
                }
            }

            // Live summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Summary("Good", okN.toString())
                    Summary("Rejected", rejectedN.toString())
                    Summary("Rej %", pct(rate), rejColor(rate))
                }
            }

            // Downtime
            Section("Downtime") {
                NumField("Minutes lost", downMin, { downMin = it }, Modifier.fillMaxWidth())
                if ((downMin.toIntOrNull() ?: 0) > 0) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Rej.downtimeReasons.drop(1)) { r ->
                            FilterChip(selected = downReason == r, onClick = { downReason = r }, label = { Text(r) })
                        }
                    }
                }
            }

            Section("Notes") {
                OutlinedTextField(
                    operator, { operator = it }, label = { Text("Operator / supervisor") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Suggest(operators) { operator = it }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    remarks, { remarks = it }, label = { Text("Remarks") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = st) }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter(Char::isDigit).take(7)) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun Suggest(items: List<String>, onPick: (String) -> Unit) {
    if (items.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        items(items) { s -> SuggestionChip(onClick = { onPick(s) }, label = { Text(s) }) }
    }
}

@Composable
private fun Counter(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton(onClick = { if (value > 0) onChange(value - 1) }) {
            Icon(Icons.Default.Remove, "Decrease $label")
        }
        Text(
            value.toString(), Modifier.width(52.dp), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }) {
            Icon(Icons.Default.Add, "Increase $label")
        }
    }
}

@Composable
private fun Summary(label: String, value: String, color: androidx.compose.ui.graphics.Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, style = MaterialTheme.typography.titleLarge,
            color = color ?: MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}
