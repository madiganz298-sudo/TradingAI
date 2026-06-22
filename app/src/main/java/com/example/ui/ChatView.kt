package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(viewModel: ChatViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAILoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to lowest message on entry or change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asisten TradeAI Pro", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(Icons.Default.Delete, "Bersihkan Chat", tint = BearRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Dialogue list area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(messages) { message ->
                    val isUser = message.role == "user"
                    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = alignment
                    ) {
                        ChatBubble(role = message.role, content = message.message)
                    }
                }

                // Show dynamic typing indicator
                if (isAiLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryGold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analis sedang mengetik...", color = TextGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Quick advice prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Jelaskan FVG", "Apa itu Order Block?", "Tips Risiko Trading").forEach { query ->
                    Text(
                        text = query,
                        fontSize = 11.sp,
                        color = PrimaryGold,
                        modifier = Modifier
                            .background(DarkGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                textInput = query
                                coroutineScope.launch {
                                    viewModel.sendMessage(query)
                                    textInput = ""
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Control inputs area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(SurfaceDark, RoundedCornerShape(24.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Tanyakan analisa teknikal, ICT, atau fundamental...", color = TextGray, fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val input = textInput.trim()
                        if (input.isNotEmpty()) {
                            viewModel.sendMessage(input)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryGold, RoundedCornerShape(22.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(role: String, content: String) {
    val isUser = role == "user"
    
    val bubbleColor = if (isUser) DarkGold.copy(alpha = 0.7f) else SurfaceDark
    val borderClr = if (isUser) PrimaryGold else BorderDark
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        shape = shape,
        modifier = Modifier
            .widthIn(max = 290.dp)
            .border(1.dp, borderClr, shape)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = content,
                color = TextWhite,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
