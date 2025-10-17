// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// SWIFT_EXPORT_CONFIG: packageRoot=flattened
// FILE: coroutines_demo.kt

package flattened

import kotlinx.coroutines.*

suspend fun testSuspendFunction(): Int = 42
