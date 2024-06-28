// KT-64622: [K/N] `ar` utility fails with symbol `@` in module/klib name on Linux with static cache
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_arm64
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE && target=linux_x64
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE && target=linux_arm64

// MODULE: lib@name
// FILE: lib.kt

fun foo() = "OK"

// MODULE: main(lib@name)
// FILE: main.kt

fun box() = foo()