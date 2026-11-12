// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE
// DUMP_EXPECT_ACTUAL_FILES

// MODULE: common
// FILE: expect.kt
package test

expect class Missing() {
    fun foo(): String
}

expect fun missingFun(): Int

expect fun presentFun(): Int

// MODULE: main()()(common)
// FILE: actual.kt
package test

actual fun presentFun(): Int = 1

class User(val missing: Missing)
