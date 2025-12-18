package com.example.superproxy.jni

object Tun2Socks {

    init {
        // This loads the native library. The name must match the one you build.
        // For example, if you build `libhev-tunnel.so`, you load "hev-tunnel".
        System.loadLibrary("hev-tunnel")
    }

    /**
     * Starts the native tun2socks process.
     * This is a blocking call, so it should be run in a background thread.
     *
     * @param tunFd The file descriptor of the TUN interface.
     * @param tunMtu The MTU of the TUN interface.
     * @param tunIp The IP address of the TUN interface.
     * @param tunGw The gateway address of the virtual network.
     * @param tunMask The netmask of the virtual network.
     * @param proxyAddress The IP or hostname of the SOCKS5 proxy server.
     * @param proxyPort The port of the SOCKS5 proxy server.
     * @return 0 on success, a negative value on failure.
     */
    external fun start(
        tunFd: Int,
        tunMtu: Int,
        tunIp: String,
        tunGw: String,
        tunMask: String,
        proxyAddress: String,
        proxyPort: Int
    ): Int

    /**
     * Stops the native tun2socks process.
     * This should be called to gracefully shut down the native layer.
     *
     * @return 0 on success, a negative value on failure.
     */
    external fun stop(): Int
}
