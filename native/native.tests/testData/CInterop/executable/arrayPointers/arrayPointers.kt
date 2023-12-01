@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import arrayPointers.*
import kotlin.test.*
import kotlinx.cinterop.*

fun main() {
    arrayPointer = globalArray
    assertEquals(globalArray[0], arrayPointer!![0])
    arrayPointer!![0] = 15
    assertEquals(15, globalArray[0])

    memScoped {
        val struct = alloc<StructWithArrayPtr>()
        struct.arrayPointer = globalArray
        assertEquals(globalArray[0], struct.arrayPointer!![0])
    }
}
