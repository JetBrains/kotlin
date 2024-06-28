// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-56173

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun replaceMeWithDefault(s: String = "OK"): String

fun commonBox(): String = replaceMeWithDefault("Fail")

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual fun replaceMeWithDefault(s: String): String = s

fun box(): String {
    return commonBox()
}
