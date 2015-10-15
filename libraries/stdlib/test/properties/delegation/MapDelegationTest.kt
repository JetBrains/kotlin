package test.properties.delegation.map

import org.junit.Test as test
import java.util.HashMap
import kotlin.properties.*
import kotlin.reflect.KProperty
import kotlin.test.*

data class B(val a: Int)

class MapValWithDifferentTypesTest() {
    val map = hashMapOf("a" to "a", "b" to 1, "c" to B(1), "d" to null)
    val a by Delegates.mapVal<String>(map)
    val b by Delegates.mapVal<Int>(map)
    val c by Delegates.mapVal<Any>(map)
    val d by Delegates.mapVal<Int?>(map)

    @test fun doTest() {
        assertTrue(a == "a", "fail at 'a'")
        assertTrue(b == 1, "fail at 'b'")
        assertTrue(c == B(1), "fail at 'c'")
        assertTrue(d == null, "fail at 'd'")
    }
}

class MapVarWithDifferentTypesTest() {
    val map: HashMap<String, Any?> = hashMapOf("a" to "a", "b" to 1, "c" to B(1), "d" to "d")
    var a: String by Delegates.mapVar(map)
    var b: Int by Delegates.mapVar(map)
    var c by Delegates.mapVar<Any>(map)
    var d by Delegates.mapVar<String?>(map)

    @test fun doTest() {
        a = "aa"
        b = 11
        c = B(11)
        d = null
        assertTrue(a == "aa", "fail at 'a'")
        assertTrue(b == 11, "fail at 'b'")
        assertTrue(c == B(11), "fail at 'c'")
        assertTrue(d == null, "fail at  'd'")

        map["a"] = null
        a // fails { a } // does not fail due to KT-8135
    }
}

class MapNullableKeyTest {
    val map = hashMapOf<Any?, Any?>(null to "null")
    var a by FixedMapVar<Any?, Any?, Any?>(map, key = { desc -> null }, default = {ref, desc -> null})

    @test fun doTest() {
        assertTrue(a == "null", "fail at 'a'")
        a = "foo"
        assertTrue(a == "foo", "fail at 'a' after set")
    }
}

class MapPropertyStringTest() {
    val map = hashMapOf<String, Any?>("a" to "a", "b" to "b", "c" to "c")
    val a: String by Delegates.mapVal(map)
    var b by Delegates.mapVar<String>(map)
    val c by Delegates.mapVal<String>(map)

    @test fun doTest() {
        b = "newB"
        assertTrue(a == "a", "fail at 'a'")
        assertTrue(b == "newB", "fail at 'b'")
        assertTrue(c == "c", "fail at 'c'")
    }
}

class MapValWithDefaultTest() {
    val map = hashMapOf<String, String>()
    val a: String by Delegates.mapVal(map, default = { ref, desc -> "aDefault" })
    val b: String by FixedMapVal(map, default = { ref: MapValWithDefaultTest, desc: String -> "bDefault" }, key = {"b"})
    val c: String by FixedMapVal(map, default = { ref: MapValWithDefaultTest, desc: String -> "cDefault" }, key = { desc -> desc.name })

    @test fun doTest() {
        assertTrue(a == "aDefault", "fail at 'a'")
        assertTrue(b == "bDefault", "fail at 'b'")
        assertTrue(c == "cDefault", "fail at 'c'")
    }
}

class MapVarWithDefaultTest() {
    val map = hashMapOf<String, Any?>()
    var a: String by Delegates.mapVar(map, default = {ref, desc -> "aDefault" })
    var b: String by FixedMapVar(map, default = {ref: Any?, desc: String -> "bDefault" }, key = {"b"})
    var c: String by FixedMapVar(map, default = {ref: Any?, desc: String -> "cDefault" }, key = { desc -> desc.name })

    @test fun doTest() {
        assertTrue(a == "aDefault", "fail at 'a'")
        assertTrue(b == "bDefault", "fail at 'b'")
        assertTrue(c == "cDefault", "fail at 'c'")
        a = "a"
        b = "b"
        c = "c"
        assertTrue(a == "a", "fail at 'a' after set")
        assertTrue(b == "b", "fail at 'b' after set")
        assertTrue(c == "c", "fail at 'c' after set")
    }
}

class MapPropertyKeyTest() {
    val map = hashMapOf<String, Any?>("a" to "a", "b" to "b")
    val a by FixedMapVal<Any?, String, String>(map, key = {"a"})
    var b by FixedMapVar<Any?, String, String>(map, key = {"b"})

    @test fun doTest() {
        b = "c"
        assertTrue(a == "a", "fail at 'a'")
        assertTrue(b == "c", "fail at 'b'")
    }
}

class MapPropertyFunctionTest() {
    val map = hashMapOf<String, Any?>("aDesc" to "a", "bDesc" to "b")
    val a by FixedMapVal<Any?, String, String>(map, { desc -> "${desc.name}Desc" })
    var b by FixedMapVar<Any?, String, String>(map, { desc -> "${desc.name}Desc" })

    @test fun doTest() {
        b = "c"
        assertTrue(a == "a", "fail at 'a'")
        assertTrue(b == "c", "fail at 'b' after set")
    }
}

val mapVal = object : MapVal<MapPropertyCustomTest, String, String>() {
    override fun map(ref: MapPropertyCustomTest) = ref.map
    override fun key(property: KProperty<*>) = "${property.name}Desc"
}

val mapVar = object : MapVar<MapPropertyCustomTest, String, String>() {
    override fun map(ref: MapPropertyCustomTest) = ref.map
    override fun key(property: KProperty<*>) = "${property.name}Desc"
}

class MapPropertyCustomTest() {
    val map = hashMapOf<String, Any?>("aDesc" to "a", "bDesc" to "b")
    val a by mapVal
    var b by mapVar

    @test fun doTest() {
        b = "newB"
        assertTrue(a == "a", "fail at 'a'")
        assertTrue(b == "newB", "fail at 'b' after set")
    }
}

val mapValWithDefault = object : MapVal<MapPropertyCustomWithDefaultTest, String, String>() {
    override fun map(ref: MapPropertyCustomWithDefaultTest) = ref.map
    override fun key(property: KProperty<*>) = property.name

    override fun default(ref: MapPropertyCustomWithDefaultTest, key: KProperty<*>) = "default"
}

val mapVarWithDefault = object : MapVar<MapPropertyCustomWithDefaultTest, String, String>() {
    override fun map(ref: MapPropertyCustomWithDefaultTest) = ref.map
    override fun key(property: KProperty<*>) = property.name

    override fun default(ref: MapPropertyCustomWithDefaultTest, key: KProperty<*>) = "default"
}

class MapPropertyCustomWithDefaultTest() {
    val map = hashMapOf<String, Any?>()
    val a by mapValWithDefault
    var b by mapVarWithDefault

    @test fun doTest() {
        assertTrue(a == "default", "fail at 'a'")
        assertTrue(b == "default", "fail at 'b'")
        b = "c"
        assertTrue(b == "c", "fail at 'b' after set")
    }
}
