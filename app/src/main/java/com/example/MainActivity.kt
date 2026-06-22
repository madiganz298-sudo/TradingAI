package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ChartView
import com.example.ui.ChartViewModel
import com.example.ui.ChatView
import com.example.ui.ChatViewModel
import com.example.ui.HomeView
import com.example.ui.SettingsView
import com.example.ui.SettingsViewModel
import com.example.ui.SplashView
import com.example.ui.WatchlistViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextGray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. SPLASH SCREEN ROUTE
                    composable("splash") {
                        SplashView(onSplashFinished = {
                            navController.navigate("main_tabs") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }

                    // 2. MAIN BOTTOM NAV DASHBOARD ROUTE
                    composable("main_tabs") {
                        MainNavigationContainer(
                            onSymbolSelected = { symbol ->
                                navController.navigate("chart_detail/$symbol")
                            }
                        )
                    }

                    // 3. CANDLESTICK CHART DETAILS ROUTE
                    composable(
                        route = "chart_detail/{symbol}",
                        arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val symbol = backStackEntry.arguments?.getString("symbol") ?: "AAPL"
                        val chartViewModel: ChartViewModel = viewModel()
                        
                        ChartView(
                            symbol = symbol,
                            viewModel = chartViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    onSymbolSelected: (String) -> Unit
) {
    val navController = rememberNavController()
    
    // Instantiate sharedViewModels here
    val watchlistViewModel: WatchlistViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val navItems = listOf(
        NavigationItem("watchlist", Icons.Default.List, "Watchlist"),
        NavigationItem("chat", Icons.Default.Send, "AI Chat"),
        NavigationItem("settings", Icons.Default.Settings, "Settings")
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "watchlist"

            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("watchlist") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Color.Black else PrimaryGold
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                color = if (isSelected) PrimaryGold else TextGray,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryGold
                        ),
                        modifier = Modifier.testTag("nav_${item.route}")
                    )
                }
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "watchlist",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("watchlist") {
                HomeView(
                    viewModel = watchlistViewModel,
                    onSymbolSelected = onSymbolSelected
                )
            }
            
            composable("chat") {
                ChatView(viewModel = chatViewModel)
            }
            
            composable("settings") {
                SettingsView(viewModel = settingsViewModel)
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
