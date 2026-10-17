package com.personalai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class ChatLine(val fromUser: Boolean, val text: String)

class MainActivity : ComponentActivity() {

    private val bridge by lazy { LlmBridge(applicationContext) }
    private val dispatcher by lazy { ActionDispatcher(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AssistantScreen(bridge, dispatcher)
            }
        }
    }
}

@Composable
fun AssistantScreen(bridge: LlmBridge, dispatcher: ActionDispatcher) {
    val messages = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isThinking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Personal Assistant", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(messages) { line ->
                Text(
                    text = (if (line.fromUser) "You: " else "Assistant: ") + line.text,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (isThinking) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text("Tell me what to do...") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val text = input
                if (text.isBlank()) return@Button
                input = ""
                messages.add(ChatLine(true, text))
                isThinking = true
                scope.launch {
                    val result = bridge.handleUserMessage(text)
                    messages.add(ChatLine(false, result.say))
                    if (result.tool != "answer_only") {
                        dispatcher.dispatch(result.tool, result.args)
                    }
                    isThinking = false
                }
            }) {
                Text("Send")
            }
        }
    }
}
