package com.example.superproxy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.superproxy.service.ProxyVpnService

data class ProxyState(
    val ip: String = "192.168.1.1",
    val port: String = "1080",
    val protocol: String = "SOCKS5",
    val username: String = "",
    val password: String = "",
    val isRunning: Boolean = false,
    val selectedApps: Set<String> = emptySet()
)

@Composable
fun MainScreen(
    state: ProxyState,
    onEvent: (MainScreenEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Super Proxy") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.ip,
                onValueChange = { onEvent(MainScreenEvent.SetIp(it)) },
                label = { Text("Proxy IP") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.port,
                onValueChange = { onEvent(MainScreenEvent.SetPort(it)) },
                label = { Text("Proxy Port") },
                modifier = Modifier.fillMaxWidth()
            )
            // In a real app, you might use a DropdownMenu for protocol selection
            OutlinedTextField(
                value = state.protocol,
                onValueChange = { onEvent(MainScreenEvent.SetProtocol(it)) },
                label = { Text("Protocol (SOCKS5/HTTP)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { onEvent(MainScreenEvent.SetUsername(it)) },
                label = { Text("Username (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onEvent(MainScreenEvent.SetPassword(it)) },
                label = { Text("Password (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (state.isRunning) {
                        onEvent(MainScreenEvent.StopVpn)
                    } else {
                        onEvent(MainScreenEvent.StartVpn)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (state.isRunning) "Stop" else "Start")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Per-App Proxy (Select apps to proxy)", style = MaterialTheme.typography.titleMedium)
            // This is a placeholder for the app list. A real implementation would
            // query the package manager for a list of installed applications.
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Placeholder items
                items(5) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Checkbox(checked = false, onCheckedChange = {})
                         Text("App ${index + 1}")
                    }
                }
            }
        }
    }
}

sealed class MainScreenEvent {
    data class SetIp(val ip: String) : MainScreenEvent()
    data class SetPort(val port: String) : MainScreenEvent()
    data class SetProtocol(val protocol: String) : MainScreenEvent()
    data class SetUsername(val username: String) : MainScreenEvent()
    data class SetPassword(val password: String) : MainScreenEvent()
    object StartVpn : MainScreenEvent()
    object StopVpn : MainScreenEvent()
}
