// A five-source-set hierarchy with an intermediate source set shared by two of three leaves:
//
//                  commonMain
//                 /          \
//          jvmAndJsMain      nativeMain
//            /      \
//       jvmMain    jsMain
//

// MODULE: commonMain
// PLATFORM: COMMON
// FILE: pkg/Common.kt
package pkg

expect fun platformName(): String

class Shared(val id: Int)

typealias commonString = String

class SomeClass : Throwable()

// MODULE: jvmAndJsMain()()(commonMain)
// PLATFORM: COMMON
// FILE: pkg/JvmAndJs.kt
package pkg

actual fun platformName(): String = "JVM or JS"

class IntermediateOnly(val value: String)

// MODULE: jvmMain()()(jvmAndJsMain)
// FILE: pkg/Jvm.kt
package pkg

fun jvmOnly(): Int = 42

// MODULE: jsMain()()(jvmAndJsMain)
// FILE: pkg/Js.kt
package pkg

fun jsOnly(): Int = 7

// MODULE: nativeMain()()(commonMain)
// PLATFORM: NATIVE
// FILE: pkg/Native.kt
package pkg

actual fun platformName(): String = "Native"

fun nativeOnly(): Int = 99
