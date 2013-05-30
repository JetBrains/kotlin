package test.properties.delegation

import java.util.HashMap
import kotlin.properties.*

class MapDelegationTest(): DelegationTestBase() {

    fun testMapPropertyString() {
        doTest(TestMapPropertyString())
    }

    fun testMapValWithDifferentTypes() {
        doTest(TestMapValWithDifferentTypes())
    }

    fun testMapVarWithDifferentTypes() {
        doTest(TestMapVarWithDifferentTypes())
    }

    fun testNullableKey() {
        doTest(TestNullableKey())
    }

    fun testMapPropertyKey() {
        doTest(TestMapPropertyKey())
    }

    fun testMapPropertyFunction() {
        doTest(TestMapPropertyFunction())
    }

    fun testMapPropertyCustom() {
        doTest(TestMapPropertyCustom())
    }

    fun testMapValWithDefault() {
        doTest(TestMapValWithDefault())
    }

    fun testMapVarWithDefault() {
        doTest(TestMapVarWithDefault())
    }

    fun testMapPropertyCustomWithDefault() {
        doTest(TestMapPropertyCustomWithDefault())
    }
}

class TestMapValWithDifferentTypes(): WithBox {
    val map = hashMapOf("a" to "a", "b" to 1, "c" to A(1), "d" to null)
    val a by Delegates.mapVal<String>(map)
    val b by Delegates.mapVal<Int>(map)
    val c by Delegates.mapVal<Any>(map)
    val d by Delegates.mapVal<Int?>(map)

    override fun box(): String {
        if (a != "a") return "fail at 'a'"
        if (b != 1) return "fail at 'b'"
        if (c != A(1)) return "fail at 'c'"
        if (d != null) return "fail at 'd'"
        return "OK"
    }

    data class A(val a: Int)
}

class TestMapVarWithDifferentTypes(): WithBox {
    val map: HashMap<String, Any?> = hashMapOf("a" to "a", "b" to 1, "c" to A(1), "d" to "d")
    var a by Delegates.mapVar<String>(map)
    var b by Delegates.mapVar<Int>(map)
    var c by Delegates.mapVar<Any>(map)
    var d by Delegates.mapVar<String?>(map)

    override fun box(): String {
        a = "aa"
        b = 11
        c = A(11)
        d = null
        if (a != "aa") return "fail at 'a'"
        if (b != 11) return "fail at 'b'"
        if (c != A(11)) return "fail at 'c'"
        if (d != null) return "fail at  'd'"
        return "OK"
    }

    data class A(val a: Int)
}

class TestNullableKey: WithBox {
    val map = hashMapOf(null:Any? to "null": Any?)
    var a by FixedMapVar<Any?, Any?, Any?>(map, key = { desc -> null }, default = {ref, desc -> null})

    override fun box(): String {
        if (a != "null") return "fail at 'a'"
        a = "foo"
        if (a != "foo") return "fail at 'a' after set"
        return "OK"
    }
}

class TestMapPropertyString(): WithBox {
    val map = hashMapOf("a" to "a", "b" to "b", "c" to "c":Any?)
    val a by Delegates.mapVal<String>(map)
    var b by Delegates.mapVar<String>(map)
    val c by Delegates.mapVal<String>(map)

    override fun box(): String {
        b = "newB"
        if (a != "a") return "fail at 'a'"
        if (b != "newB") return "fail at 'b'"
        if (c != "c") return "fail at 'c'"
        return "OK"
    }
}

class TestMapValWithDefault(): WithBox {
    val map = hashMapOf<String, String>()
    val a by Delegates.mapVal<String>(map, default = { ref, desc -> "aDefault" })
    val b by FixedMapVal<TestMapValWithDefault, String, String>(map, default = { ref, desc -> "bDefault" }, key = {"b"})
    val c by FixedMapVal<TestMapValWithDefault, String, String>(map, default = { ref, desc -> "cDefault" }, key = { desc -> desc.name })

    override fun box(): String {
        if (a != "aDefault") return "fail at 'a'"
        if (b != "bDefault") return "fail at 'b'"
        if (c != "cDefault") return "fail at 'c'"
        return "OK"
    }
}

class TestMapVarWithDefault(): WithBox {
    val map = hashMapOf<String, Any?>()
    var a: String by Delegates.mapVar(map, default = {ref, desc -> "aDefault" })
    var b: String by FixedMapVar<Any?, String, String>(map, default = {ref, desc -> "bDefault" }, key = {"b"})
    var c: String by FixedMapVar<Any?, String, String>(map, default = {ref, desc -> "cDefault" }, key = { desc -> desc.name })

    override fun box(): String {
        if (a != "aDefault") return "fail at 'a'"
        if (b != "bDefault") return "fail at 'b'"
        if (c != "cDefault") return "fail at 'c'"
        a = "a"
        b = "b"
        c = "c"
        if (a != "a") return "fail at 'a' after set"
        if (b != "b") return "fail at 'b' after set"
        if (c != "c") return "fail at 'c' after set"
        return "OK"
    }
}

class TestMapPropertyKey(): WithBox {
    val map = hashMapOf("a" to "a", "b" to "b" : Any?)
    val a by FixedMapVal<Any?, String, String>(map, key = {"a"})
    var b by FixedMapVar<Any?, String, String>(map, key = {"b"})

    override fun box(): String {
        b = "c"
        if (a != "a") return "fail at 'a'"
        if (b != "c") return "fail at 'b'"
        return "OK"
    }
}

class TestMapPropertyFunction(): WithBox {
    val map = hashMapOf("aDesc" to "a", "bDesc" to "b": Any?)
    val a by FixedMapVal<Any?, String, String>(map, { desc -> "${desc.name}Desc" })
    var b by FixedMapVar<Any?, String, String>(map, { desc -> "${desc.name}Desc" })

    override fun box(): String {
        b = "c"
        if (a != "a") return "fail at 'a'"
        if (b != "c") return "fail at 'b' after set"
        return "OK"
    }
}

val mapVal = object: MapVal<TestMapPropertyCustom, String, String>() {
    override fun map(ref: TestMapPropertyCustom) = ref.map
    override fun key(desc: PropertyMetadata) = "${desc.name}Desc"
}

val mapVar = object : MapVar<TestMapPropertyCustom, String, String>() {
    override fun map(ref: TestMapPropertyCustom) = ref.map
    override fun key(desc: PropertyMetadata) = "${desc.name}Desc"
}

class TestMapPropertyCustom(): WithBox {
    val map = hashMapOf("aDesc" to "a", "bDesc" to "b":Any?)
    val a by mapVal
    var b by mapVar

    override fun box(): String {
        b = "newB"
        if (a != "a") return "fail at 'a'"
        if (b != "newB") return "fail at 'b' after set"
        return "OK"
    }
}

val mapValWithDefault = object : MapVal<TestMapPropertyCustomWithDefault, String, String>() {
    override fun map(ref: TestMapPropertyCustomWithDefault) = ref.map
    override fun key(desc: PropertyMetadata) = desc.name

    override fun default(ref: TestMapPropertyCustomWithDefault, key: PropertyMetadata) = "default"
}

val mapVarWithDefault = object : MapVar<TestMapPropertyCustomWithDefault, String, String>() {
    override fun map(ref: TestMapPropertyCustomWithDefault) = ref.map
    override fun key(desc: PropertyMetadata) = desc.name

    override fun default(ref: TestMapPropertyCustomWithDefault, key: PropertyMetadata) = "default"
}

class TestMapPropertyCustomWithDefault(): WithBox {
    val map = hashMapOf<String, Any?>()
    val a by mapValWithDefault
    var b by mapVarWithDefault

    override fun box(): String {
        if (a != "default") return "fail at 'a'"
        if (b != "default") return "fail at 'b'"
        b = "c"
        if (b != "c") return "fail at 'b' after set"
        return "OK"
    }
}
