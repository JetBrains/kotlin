// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: coroutines.kt

import kotlinx.coroutines.*

fun demo() = runBlocking {
    5
}
