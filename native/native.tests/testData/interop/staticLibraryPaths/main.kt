@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.test.*
import staticLibraryPaths.*

fun main() {
    assertEquals(42, answer())
}
