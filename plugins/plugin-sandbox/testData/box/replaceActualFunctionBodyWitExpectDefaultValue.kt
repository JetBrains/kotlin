// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-56173

// MODULE: common
// FILE: common.kt

expect fun replaceMeWithDefault(s: String = "OK"): String

fun commonBox(): String = replaceMeWithDefault("Fail")

// MODULE: jvm()()(common)
// FILE: main.kt

actual fun replaceMeWithDefault(s: String): String = s

fun box(): String {
    return commonBox()
}
