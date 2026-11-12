// LANGUAGE: +MultiPlatformProjects
// DUMP_EXPECT_ACTUAL_FILES

// MODULE: common
// FILE: expect.kt
package test

expect class Platform() {
    val name: String
    fun describe(): String
}

expect fun platformName(): String

expect val platformVersion: Int

// FILE: expectBox.kt
package test

expect class Box<T>(value: T) {
    val value: T
}

// MODULE: main()()(common)
// FILE: actualPlatform.kt
package test

actual class Platform actual constructor() {
    actual val name: String = "JVM"
    actual fun describe(): String = name
}

// FILE: actualTopLevel.kt
package test

actual fun platformName(): String = "JVM"

actual val platformVersion: Int = 17

// FILE: actualBox.kt
package test

actual typealias Box<T> = BoxImpl<T>

// FILE: boxImpl.kt
package test

class BoxImpl<T>(val value: T)
