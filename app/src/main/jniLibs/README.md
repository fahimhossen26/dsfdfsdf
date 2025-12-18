# Native Library Integration (tun2socks)

This Android application relies on a native `tun2socks` library to handle the low-level packet processing required to forward traffic from the Android `VpnService` to a proxy.

The Kotlin JNI bridge (`Tun2Socks.kt`) is set up to work with a library that exposes specific C-style functions. Our implementation is designed to work with **`hev-socks5-tunnel`**, a lightweight and efficient `tun2socks` implementation written in C.

## 1. Why You Need a Native Library

The Android `VpnService` provides raw IP packets from the device's network stack. To send this traffic to a SOCKS5 or HTTP proxy, each TCP packet must be:
1.  Read from the TUN interface.
2.  Interpreted as a TCP/IP packet.
3.  Unpacked to get the original destination address and port.
4.  Wrapped in the appropriate proxy protocol (e.g., SOCKS5).
5.  Sent to the proxy server.

This entire process is called `tun2socks`, and performing it efficiently requires low-level C or Go code.

## 2. Compiling `hev-socks5-tunnel` for Android

You must compile the `hev-socks5-tunnel` library yourself for each Android ABI you intend to support (e.g., `arm64-v8a`, `armeabi-v7a`, `x86_64`).

### Prerequisites
- Android NDK (installed via Android Studio's SDK Manager).
- Standard build tools (`git`, `cmake`, `make`).

### Steps:
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/heiher/hev-socks5-tunnel.git
    cd hev-socks5-tunnel
    ```

2.  **Create a JNI wrapper:** The `hev-socks5-tunnel` library is a command-line tool. You need to create a small C file to make it work as a library with the JNI functions our app expects.

    Create a file named `jni/main.c` in the `hev-socks5-tunnel` project with the following content:

    ```c
    #include <jni.h>
    #include "hev-main.h"

    JNIEXPORT jint JNICALL
    Java_com_example_superproxy_jni_Tun2Socks_start(JNIEnv *env, jobject thiz, jint tun_fd, jint tun_mtu,
                                                 const jchar *tun_ip, const jchar *tun_gw, const jchar *tun_mask,
                                                 const jchar *proxy_address, jint proxy_port) {
        // Construct the arguments for hev-main
        // Example: hev-socks5-tunnel -s <proxy_address> -p <proxy_port> -t <tun_fd> ...
        // This part needs to be adapted to how hev-main parses its arguments.
        // A common approach is to manually build the argv array.
        const char *ip = (*env)->GetStringUTFChars(env, tun_ip, NULL);
        // ... get other strings ...

        // This is a simplified example. You will need to construct the full
        // command line arguments array (`argv`) for `hev_main`.
        char p_port[16];
        sprintf(p_port, "%d", proxy_port);
        char p_fd[16];
        sprintf(p_fd, "%d", tun_fd);
        
        const char *argv[] = {
            "hev-socks5-tunnel",
            "-s", (const char*)proxy_address,
            "-p", p_port,
            "-t", p_fd,
            // Add other parameters like MTU, etc.
            NULL
        };
        int argc = sizeof(argv) / sizeof(argv[0]) - 1;

        // The main loop of hev-socks5-tunnel
        return hev_main(argc, (char **)argv);
    }

    JNIEXPORT jint JNICALL
    Java_com_example_superproxy_jni_Tun2Socks_stop(JNIEnv *env, jobject thiz) {
        // Signal the hev_main loop to terminate.
        // `hev-socks5-tunnel` uses signal handling for this.
        hev_main_quit();
        return 0;
    }
    ```
    **Note:** This wrapper is a template. You will need to adjust it to match the exact command-line arguments and termination logic of `hev-socks5-tunnel`.

3.  **Build using NDK:** Use a `CMakeLists.txt` or a standalone NDK build script to compile the project into a shared library (`.so` file). You will need to target different ABIs.

4.  **Place the Library:**
    Once compiled, you will have a file like `libhev-tunnel.so`. Copy this file into the correct ABI directory within your Android Studio project:

    ```
    app/src/main/jniLibs/
    ├── arm64-v8a/
    │   └── libhev-tunnel.so
    ├── armeabi-v7a/
    │   └── libhev-tunnel.so
    └── x86_64/
        └── libhev-tunnel.so
    ```

    The `Tun2Socks.kt` object will then be able to load and use it.

## 3. Alternative Libraries
- **`badvpn-tun2socks`**: A classic choice, but can be more complex to build.
- **Go-based libraries (`go-tun2socks`)**: If you are more comfortable with Go, you can use `gomobile` to build a library that can be called from Kotlin. The JNI bridge would be slightly different.
