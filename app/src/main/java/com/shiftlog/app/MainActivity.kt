package com.shiftlog.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val entries: StateFlow<List<ShiftEntry>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(e: ShiftEntry) = viewModelScope.launch { dao.upsert(e) }
    fun delete(e: ShiftEntry) = viewModelScope.launch { dao.delete(e) }
    suspend fun get(id: Long): ShiftEntry? = dao.get(id)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftLogTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = viewModel()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm,
                            onNew = { nav.navigate("form/0") },
                            onEdit = { nav.navigate("form/$it") },
                            onReports = { nav.navigate("report") }
                        )
                    }
                    composable(
                        "form/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { back ->
                        FormScreen(vm, back.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
                    }
                    composable("report") { ReportScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
