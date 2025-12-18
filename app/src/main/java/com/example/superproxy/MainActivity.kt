package com.example.superproxy

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.superproxy.service.ProxyVpnService
import com.example.superproxy.ui.MainScreen
import com.example.superproxy.ui.MainScreenEvent
import com.example.superproxy.ui.ProxyState
import com.example.superproxy.ui.theme.SuperProxyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// A simple ViewModel to hold the UI state.
class MainViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(ProxyState())
    val uiState: StateFlow<ProxyState> = _uiState.asStateFlow()

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.SetIp -> _uiState.update { it.copy(ip = event.ip) }
            is MainScreenEvent.SetPort -> _uiState.update { it.copy(port = event.port) }
            is MainScreenEvent.SetProtocol -> _uiState.update { it.copy(protocol = event.protocol) }
            is MainScreenEvent.SetUsername -> _uiState.update { it.copy(username = event.username) }
            is MainScreenEvent.SetPassword -> _uiState.update { it.copy(password = event.password) }
            // VPN start/stop is handled in the activity for now to get the result
            else -> Unit 
        }
    }
    
    fun setVpnRunning(isRunning: Boolean) {
        _uiState.update { it.copy(isRunning = isRunning) }
    }
}


class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            // Update the running state based on a static variable in the service
            // This is a simple way to keep UI in sync. A better way is to use a bound service or broadcasts.
            viewModel.setVpnRunning(ProxyVpnService.isRunning)

            SuperProxyTheme {
                MainScreen(
                    state = uiState,
                    onEvent = { event ->
                        when(event) {
                            MainScreenEvent.StartVpn -> checkVpnPermission()
                            MainScreenEvent.StopVpn -> stopVpnService()
                            else -> viewModel.onEvent(event)
                        }
                    }
                )
            }
        }
    }

    private fun checkVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val state = viewModel.uiState.value
        val intent = Intent(this, ProxyVpnService::class.java).apply {
            putExtra(ProxyVpnService.EXTRA_IP, state.ip)
            putExtra(ProxyVpnService.EXTRA_PORT, state.port)
            putExtra(ProxyVpnService.EXTRA_PROTOCOL, state.protocol)
            // Add other details like username, password, allowed apps
        }
        startService(intent)
        viewModel.setVpnRunning(true)
    }

    private fun stopVpnService() {
        val intent = Intent(this, ProxyVpnService::class.java)
        stopService(intent)
        viewModel.setVpnRunning(false)
    }
}
