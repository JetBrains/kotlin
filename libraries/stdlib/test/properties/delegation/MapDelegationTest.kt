package test.properties.delegation

import org.junit.Test as test
import java.util.HashMap
import kotlin.properties.*

class MapDelegationTest(): DelegationTestBase() {

    test fun testMapPropertyString() {
        doTest(TestMapPropertyString())
    }

    test fun testMapValWithDifferentTypes() {
        doTest(TestMapValWithDifferentTypes())
    }

    test fun testMapVarWithDifferentTypes() {
        doTest(TestMapVarWithDifferentTypes())
    }

    test fun testNullableKey() {
        doTest(TestNullableKey())
    }

    test fun testMapPropertyKey() {
        doTest(TestMapPropertyKey())
    }

    test fun testMapPropertyFunction() {
        doTest(TestMapPropertyFunction())
    }

    test fun testMapPropertyCustom() {
        doTest(TestMapPropertyCustom())
    }

    test fun testMapValWithDefault() {
        doTest(TestMapValWithDefault())
    }

    test fun testMapVarWithDefault() {
        doTest(TestMapVarWithDefault())
    }

    test fun testMapPropertyCustomWithDefault() {
        doTest(TestMapPropertyCustomWithDefault())
    }
}

data class B(val a: Int)

class TestMapValWithDifferentTypes(): WithBox {
    val map = hashMapOf("a" to "a", "b" to 1, "c" to B(1), "d" to null)
    val a by Delegates.mapVal<String>(map)
    val b by Delegates.mapVal<Int>(map)
    val c by Delegates.mapVal<Any>(map)
    val d by Delegates.mapVal<Int?>(map)

    override fun box(): String {
        if (a != "a") return "fail at 'a'"
        if (b != 1) return "fail at 'b'"
        if (c != B(1)) return "fail at 'c'"
        if (d != null) return "fail at 'd'"
        return "OK"
    }
}

class TestMapVarWithDifferentTypes(): WithBox {
    val map: HashMap<String, Any?> = hashMapOf("a" to "a", "b" to 1, "c" to B(1), "d" to "d")
    var a: String by Delegates.mapVar(map)
    var b: Int by Delegates.mapVar(map)
    var c by Delegates.mapVar<Any>(map)
    var d by Delegates.mapVar<String?>(map)

    override fun box(): String {
        a = "aa"
        b = 11
        c = B(11)
        d = null
        if (a != "aa") return "fail at 'a'"
        if (b != 11) return "fail at 'b'"
        if (c != B(11)) return "fail at 'c'"
        if (d != null) return "fail at  'd'"
        return "OK"
    }
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
    val a: String by Delegates.mapVal(map)
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
    val a: String by Delegates.mapVal(map, default = { ref, desc -> "aDefault" })
    val b: String by FixedMapVal(map, default = { (ref: TestMapValWithDefault, desc: String) -> "bDefault" }, key = {"b"})
    val c: String by FixedMapVal(map, default = { (ref: TestMapValWithDefault, desc: String) -> "cDefault" }, key = { desc -> desc.name })

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
    var b: String by FixedMapVar(map, default = {(ref: Any?, desc: String) -> "bDefault" }, key = {"b"})
    var c: String by FixedMapVar(map, default = {(ref: Any?, desc: String) -> "cDefault" }, key = { desc -> desc.name })

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
