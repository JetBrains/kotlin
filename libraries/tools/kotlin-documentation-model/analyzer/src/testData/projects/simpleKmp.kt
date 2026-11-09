// MODULE: commonMain
// PLATFORM: COMMON
// FILE: pkg/Common.kt
package pkg

expect fun platformName(): String

expect val platformVersion: Int

expect class Platform

class Shared(val id: Int)

// MODULE: jvmMain()()(commonMain)
// FILE: pkg/Jvm.kt
package pkg

actual fun platformName(): String = "JVM"

actual val platformVersion: Int = 1

actual class Platform

fun jvmOnly(): Int = 42
