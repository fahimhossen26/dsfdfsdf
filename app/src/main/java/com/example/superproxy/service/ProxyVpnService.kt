package com.example.superproxy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.superproxy.jni.Tun2Socks
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class ProxyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    companion object {
        const val EXTRA_IP = "ip"
        const val EXTRA_PORT = "port"
        const val EXTRA_PROTOCOL = "protocol"
        
        @Volatile
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("ProxyVpnService", "Service starting")
        val ip = intent?.getStringExtra(EXTRA_IP) ?: "127.0.0.1"
        val port = intent?.getStringExtra(EXTRA_PORT)?.toIntOrNull() ?: 1080
        
        startForeground(1, createNotification())
        
        vpnThread = Thread {
            try {
                isRunning = true
                runVpn(ip, port)
            } catch (e: Exception) {
                Log.e("ProxyVpnService", "VPN thread error", e)
            } finally {
                Log.i("ProxyVpnService", "VPN thread stopping")
                stopVpn()
            }
        }
        vpnThread?.start()
        
        return START_STICKY
    }

    private fun runVpn(proxyIp: String, proxyPort: Int) {
        val builder = Builder()

        // Configure the TUN interface.
        val session = builder
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8") // Route DNS traffic through the VPN
            .addRoute("0.0.0.0", 0) // Route all traffic
            .setSession("SuperProxy")
            .setMtu(1500)
            // .addAllowedApplication("com.example.apptoproxy") // Uncomment for per-app proxy
            .establish() ?: throw IOException("Failed to establish VPN session")

        vpnInterface = session
        val tunFd = vpnInterface!!.fileDescriptor.asInt()

        Log.i("ProxyVpnService", "TUN interface is up. FD: $tunFd")

        // This is the bridge to the native layer.
        // The native code will read from this file descriptor, wrap packets
        // in SOCKS5/HTTP, and send them to the proxy server.
        Tun2Socks.start(
            tunFd,
            1500, // MTU
            "10.0.0.2", // TUN IP
            "10.0.0.1", // Virtual Gateway
            "255.255.255.0", // Netmask
            proxyIp,
            proxyPort
        )
        
        Log.i("ProxyVpnService", "Native tun2socks process finished.")
    }

    private fun stopVpn() {
        if (isRunning) {
            Tun2Socks.stop()
            vpnInterface?.close()
            vpnInterface = null
            vpnThread?.interrupt()
            vpnThread = null
            isRunning = false
            stopForeground(true)
            stopSelf()
            Log.i("ProxyVpnService", "VPN stopped")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ProxyVpnService"
            val descriptionText = "Proxy VPN Service Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ProxyVpnServiceChannel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "ProxyVpnServiceChannel")
                .setContentTitle("Super Proxy")
                .setContentText("Proxy is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with a proper icon
                .build()
        } else {
            // Deprecated but required for older APIs
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Super Proxy")
                .setContentText("Proxy is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }
}

// Extension function to get the integer file descriptor
fun ParcelFileDescriptor.asInt(): Int {
    val field = ParcelFileDescriptor::class.java.getDeclaredField("descriptor")
    field.isAccessible = true
    return field.getInt(this)
}
