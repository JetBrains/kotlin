// MODULE: commonMain
// PLATFORM: COMMON
// FILE: example/Common.kt
package example

expect class Alias

expect class Platform {
    fun id(): Int

    val name: String
}

expect fun platformName(): String

expect val platformVersion: Int

class Shared(val id: Int)

// MODULE: intermediateMain()()(commonMain)
// PLATFORM: COMMON
// FILE: example/Intermediate.kt
package example

class Intermediate(val label: String)

actual class Platform {
    actual fun id(): Int

    actual val name: String
}

actual val platformVersion: Int

// MODULE: jvmMain()()(intermediateMain)
// PLATFORM: JVM
// FILE: example/Jvm.kt
package example

actual typealias Alias = String

fun jvmOnly(): Int

actual fun platformName(): String

// MODULE: jsMain()()(intermediateMain)
// PLATFORM: JS
// FILE: example/Js.kt
package example

actual typealias Alias = String

fun jsOnly(): Int

actual fun platformName(): String
