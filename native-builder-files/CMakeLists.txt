# Minimum version of CMake
cmake_minimum_required(VERSION 3.10)

# Define the project name
project(HevTunnelWrapper)

# --- Hev-Socks5-Tunnel ---
# Add the source code of the hev-socks5-tunnel library.
# This assumes you place this CMakeLists.txt inside the root of the cloned hev-socks5-tunnel repo.
# Add all necessary .c and .h files from the original project.
# You will need to inspect its source and add all required files.
# Example:
file(GLOB HEV_SOURCES
    "hev-socks5-tunnel-*.c"
    "hev-socks5-tunnel.h"
    "hev-task-dns.c"
    "hev-task-dns.h"
    # ... and so on for all required source files
)

# --- Our JNI Wrapper ---
# Add our JNI wrapper source file
add_library(hev-tunnel SHARED main.c ${HEV_SOURCES})

# --- Find and Link Dependencies ---
# Find the standard Android library and the logging library
find_library(log-lib log)

# Link the libraries together. Our wrapper needs to link against the Android log library.
# The hev-socks5-tunnel library itself may have other dependencies like pthreads.
target_link_libraries(hev-tunnel ${log-lib} -lpthread)
