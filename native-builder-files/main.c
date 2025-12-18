#include <jni.h>
#include <string.h>
#include <malloc.h>
#include <pthread.h>

// Forward declarations for the functions from the hev-socks5-tunnel library.
// We assume these are available from the library we are linking against.
int hev_main(int argc, char *argv[]);
void hev_main_quit(void);

static pthread_t main_thread;

// A helper function to convert Java strings to C strings (char*)
// and build the argument list for hev_main.
char** build_argv(JNIEnv *env, jint tun_fd, jint tun_mtu, jstring tun_ip, jstring tun_gw, jstring tun_mask, jstring proxy_address, jint proxy_port, int* argc) {
    const char *tun_ip_c = (*env)->GetStringUTFChars(env, tun_ip, 0);
    const char *tun_gw_c = (*env)->GetStringUTFChars(env, tun_gw, 0);
    const char *tun_mask_c = (*env)->GetStringUTFChars(env, tun_mask, 0);
    const char *proxy_address_c = (*env)->GetStringUTFChars(env, proxy_address, 0);

    char tun_fd_str[16];
    char tun_mtu_str[16];
    char proxy_port_str[16];

    sprintf(tun_fd_str, "%d", tun_fd);
    sprintf(tun_mtu_str, "%d", tun_mtu);
    sprintf(proxy_port_str, "%d", proxy_port);

    // Count arguments
    *argc = 15; // "hev-socks5-tunnel", "-v", "-t", fd, "-m", mtu, "-a", ip, "-g", gw, "-n", mask, "-s", addr, "-p", port
    char **argv = (char **) malloc(sizeof(char *) * (*argc + 1));

    argv[0] = strdup("hev-socks5-tunnel");
    argv[1] = strdup("-v"); // Verbose logging
    argv[2] = strdup("-t");
    argv[3] = strdup(tun_fd_str);
    argv[4] = strdup("-m");
    argv[5] = strdup(tun_mtu_str);
    argv[6] = strdup("-a");
    argv[7] = strdup(tun_ip_c);
    argv[8] = strdup("-g");
    argv[9] = strdup(tun_gw_c);
    argv[10] = strdup("-n");
    argv[11] = strdup(tun_mask_c);
    argv[12] = strdup("-s");
    argv[13] = strdup(proxy_address_c);
    argv[14] = strdup("-p");
    argv[15] = strdup(proxy_port_str);
    argv[16] = NULL; // Null-terminate the list

    // Release the Java string resources
    (*env)->ReleaseStringUTFChars(env, tun_ip, tun_ip_c);
    (*env)->ReleaseStringUTFChars(env, tun_gw, tun_gw_c);
    (*env)->ReleaseStringUTFChars(env, tun_mask, tun_mask_c);
    (*env)->ReleaseStringUTFChars(env, proxy_address, proxy_address_c);

    return argv;
}

void free_argv(char** argv, int argc) {
    if (!argv) return;
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
}


// The main entry point that will be called from our background thread
void* thread_entry(void* arg) {
    char** argv = (char**) arg;
    int argc = 0;
    // Count the args
    while(argv[argc] != NULL) {
        argc++;
    }
    hev_main(argc, argv);
    free_argv(argv, argc);
    return NULL;
}


JNIEXPORT jint JNICALL
Java_com_example_superproxy_jni_Tun2Socks_start(JNIEnv *env, jobject thiz, jint tun_fd, jint tun_mtu,
                                                 jstring tun_ip, jstring tun_gw, jstring tun_mask,
                                                 jstring proxy_address, jint proxy_port) {
    int argc = 0;
    char **argv = build_argv(env, tun_fd, tun_mtu, tun_ip, tun_gw, tun_mask, proxy_address, proxy_port, &argc);

    // Run hev_main in a new thread to avoid blocking the JNI call forever
    int result = pthread_create(&main_thread, NULL, thread_entry, argv);
    if (result != 0) {
        // Thread creation failed
        free_argv(argv, argc);
        return -1;
    }

    pthread_detach(main_thread);
    return 0; // Success
}

JNIEXPORT jint JNICALL
Java_com_example_superproxy_jni_Tun2Socks_stop(JNIEnv *env, jobject thiz) {
    // Signal the hev_main loop to terminate.
    hev_main_quit();
    main_thread = 0;
    return 0;
}
