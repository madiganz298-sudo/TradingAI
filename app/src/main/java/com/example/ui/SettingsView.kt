package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiKeyEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val apiKeysList by viewModel.apiKeys.collectAsState()
    val isAlertChecking by viewModel.isAlertChecking.collectAsState()

    var inputKey by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("OPENROUTER") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Multi-API", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. INPUT FORM FIELD FOR NEW API KEY
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = PrimaryGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Input Multi API-Key", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Pilih Layanan API:", color = TextWhite, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("OPENROUTER", "FINNHUB", "TWELVEDATA", "NEWSAPI").forEach { service ->
                                val isSel = selectedService == service
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedService = service },
                                    label = { Text(service, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGold,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            placeholder = { Text("Masukkan API Key Layanan Anda...", fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (inputKey.isNotBlank()) {
                                    viewModel.addApiKey(selectedService, inputKey)
                                    inputKey = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan Key", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. LIST REGISTERED KEYS WITH MASKING
            item {
                Text("Daftar API-Key Tersimpan (Rotasi Aktif)", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (apiKeysList.isEmpty()) {
                item {
                    Text("Belum ada API Key khusus tersimpan. Menggunakan akun demo/sandbox internal.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                items(apiKeysList) { keyEntity ->
                    ApiKeyRowCard(
                        entity = keyEntity,
                        onDelete = { viewModel.deleteApiKey(keyEntity.id) }
                    )
                }
            }

            // 3. SECURED SCHEDULER CONTROLS (WorkManager triggers)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = PrimaryGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manajemen Alert Background", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Pasang pemicu latar belakang periodik untuk mencocokkan harga market dengan alert Anda di background.", color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = { viewModel.testManualAlertChecks(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                                enabled = !isAlertChecking,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isAlertChecking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                } else {
                                    Text("Test Run", color = TextWhite)
                                }
                            }

                            Button(
                                onClick = { viewModel.scheduleBackgroundChecks(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Jadwalkan Lokalnya", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. BRANDING IDENTITY METADATA
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Developer: M4DI~UciH4", color = PrimaryGold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Text("TradeAI Pro Engine v1.0.0 (Native Core Kotlin)", color = TextGray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyRowCard(entity: ApiKeyEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.serviceType, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                
                // Masking Key for privacy (only showing first 4 and last 4 characters digits)
                val key = entity.apiKey
                val masked = if (key.length > 8) {
                    "${key.take(4)}...${key.takeLast(4)}"
                } else {
                    "••••••••"
                }
                
                Text(masked, color = TextWhite, fontSize = 14.sp)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus Key", tint = BearRed)
            }
        }
    }
}
