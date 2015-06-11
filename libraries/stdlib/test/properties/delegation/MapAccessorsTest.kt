package test.properties.delegation

import kotlin.properties.*
import kotlin.test.*
import org.junit.Test as test

class ValByMapExtensionsTest {
    val map: Map<String, String> = hashMapOf("a" to "all", "b" to "bar", "c" to "code")
    val genericMap = mapOf<String, Any?>("i" to 1, "x" to 1.0)

    val a: String by map
    val b: String by map
    val c: Any by map
    val d: String? by map
    val e: Any by map
    val i: Int by genericMap
    val x: Double by genericMap


    test fun doTest() {
        assertEquals("all", a)
        assertEquals("bar", b)
        assertEquals("code", c)
        assertEquals(1, i)
        assertEquals(1.0, x)
        fails { d }
    }
}



class VarByMapExtensionsTest {
    val map = hashMapOf<String, Any?>("a" to "all", "b" to null, "c" to 1, "xProperty" to 1.0)
    val map2: MutableMap<String, CharSequence> = hashMapOf("a2" to "all")

    var a: String by map
    var b: Any? by map
    var c: Int by map
    var d: String? by map
    var a2: String by map2
    //var x: Int by map2  // prohibited by type system

    test fun doTest() {
        assertEquals("all", a)
        assertEquals(null, b)
        assertEquals(1, c)
        c = 2
        assertEquals(2, c)
        assertEquals(2, map["c"])


        map["c"] = "string"
        fails { c }

        map["a"] = null
        a // fails { a } // does not fail due to KT-8135

        fails { d }
        map["d"] = null
        assertEquals(null, d)
    }
}

class ValByMapAccessorExtensionsTest {
    val map: Map<String, String> = mapOf("a" to "all", "b" to "bar", "c" to "code")
    val genericMap = mapOf<String, Any?>("i" to 1, "x" to 1.0)

    val mapAccessor = map.forProperties { it.name.take(1) }
    val genericMapAccessor = genericMap.forProperties { it.name.take(1) }

    val cProperty by mapAccessor
    val xProperty by genericMapAccessor
    val zNoDefault by genericMapAccessor
    val zProperty by genericMapAccessor.withDefault { 1 }

    test fun doTest() {
        assertEquals("code", cProperty)
        assertEquals(1.0, xProperty)
        assertEquals(1, zProperty)
        fails { zNoDefault }
    }
}

class VarByMapAccessorExtensionsTest {
    val map = hashMapOf<String, Any?>("xProperty" to 1.0)

    val mapAccessor = map.forProperties { it.name + "Property" }

    var x: Double by mapAccessor.withDefault { 2.0 }
    var z: Int by mapAccessor
    var s: String by mapAccessor

    test fun doTest() {
        assertEquals(1.0, x)

        map.remove("xProperty")
        assertEquals(2.0, x)

        z = 2
        assertEquals(2, z)
        assertEquals(2, map["zProperty"])

        fails { s }
        map["sProperty"] = null
        s // fails { s }  // does not fail due to KT-8135
    }
}
