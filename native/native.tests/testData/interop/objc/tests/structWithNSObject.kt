import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

private class NSObjectSubClass : NSObject() {
    val x = 111
}

@Test
fun testStructWithNSObject() {
    memScoped {
        val struct = alloc<CStructWithNSObjects>()

        struct.any = 5
        assertEquals(5, struct.any)
        struct.any = null
        assertEquals(null, struct.any)

        struct.nsString = "hello"
        assertEquals("hello", struct.nsString)
        struct.nsString = null
        assertEquals(null, struct.nsString)

        struct.nonNullString = "world"
        assertEquals("world", struct.nonNullString)

        struct.`object` = NSObjectSubClass()
        assertEquals(111, (struct.`object` as NSObjectSubClass).x)
        struct.`object` = null
        assertEquals(null, struct.`object`)

        struct.array = null
        assertEquals(null, struct.array)
        struct.array = listOf(1, 2, 3)
        assertEquals(listOf(1, 2, 3), struct.array)

        struct.set = null
        assertEquals(null, struct.set)
        struct.set = setOf("hello", "world")
        assertEquals(setOf("hello", "world"), struct.set)

        struct.dictionary = null
        assertEquals(null, struct.dictionary)
        struct.dictionary = mapOf("k1" to "v1", "k2" to "v2")
        assertEquals(mapOf<Any?, String>("k1" to "v1", "k2" to "v2"), struct.dictionary)

        struct.mutableArray = null
        assertEquals(null, struct.mutableArray)
        struct.mutableArray = mutableListOf(1, 2)
        struct.mutableArray!! += 3
        assertEquals(mutableListOf<Any?>(1, 2, 3), struct.mutableArray)

        // Check that subtyping via Nothing-returning functions does not break compiler.
        assertFailsWith<NotImplementedError> {
            struct.any = TODO()
            struct.nsString = TODO()
            struct.nonNullString = TODO()
            struct.`object` = TODO()
            struct.array = TODO()
            struct.set = TODO()
            struct.dictionary = TODO()
            struct.mutableArray = TODO()
        }
    }
}