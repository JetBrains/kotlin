package test.properties.delegation

import java.util.HashMap
import kotlin.properties.delegation.*

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
    val a by map.readOnlyProperty<String>()
    val b by map.readOnlyProperty<Int>()
    val c by map.readOnlyProperty<Any>()
    val d by map.readOnlyProperty<Int?>()

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
    var a by map.property<String>()
    var b by map.property<Int>()
    var c by map.property<Any>()
    var d by map.property<String?>()

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
    val map = hashMapOf(null to "null")
    var a by map.property<String?> { desc -> null }

    override fun box(): String {
        if (a != "null") return "fail at 'a'"
        a = "foo"
        if (a != "foo") return "fail at 'a' after set"
        return "OK"
    }
}

class TestMapPropertyString(): WithBox {
    val map = hashMapOf("a" to "a", "b" to "b", "c" to "c")
    val a by map.readOnlyProperty<String>()
    var b by map.property<String>()
    val c by map.property<String>()

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
    val a by map.readOnlyProperty<String>(default = { "aDefault" })
    val b by map.readOnlyProperty<String>(default = { "bDefault" }, key = "b")
    val c by map.readOnlyProperty<String>(default = { "cDefault" }, key = { desc -> desc.name })

    override fun box(): String {
        if (a != "aDefault") return "fail at 'a'"
        if (b != "bDefault") return "fail at 'b'"
        if (c != "cDefault") return "fail at 'c'"
        return "OK"
    }
}

class TestMapVarWithDefault(): WithBox {
    val map = hashMapOf<String, String>()
    var a by map.property<String>(default = { "aDefault" })
    var b by map.property<String>(default = { "bDefault" }, key = "b")
    var c by map.property<String>(default = { "cDefault" }, key = { desc -> desc.name })

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
    val map = hashMapOf("a" to "a", "b" to "b")
    val a by map.readOnlyProperty<String>(key = "a")
    var b by map.property<String>(key = "b")

    override fun box(): String {
        b = "c"
        if (a != "a") return "fail at 'a'"
        if (b != "c") return "fail at 'b'"
        return "OK"
    }
}

class TestMapPropertyFunction(): WithBox {
    val map = hashMapOf("aDesc" to "a", "bDesc" to "b")
    val a by map.readOnlyProperty<String> { desc -> "${desc.name}Desc" }
    var b by map.property<String> { desc -> "${desc.name}Desc" }

    override fun box(): String {
        b = "c"
        if (a != "a") return "fail at 'a'"
        if (b != "c") return "fail at 'b' after set"
        return "OK"
    }
}

val mapVal = object : MapVal<TestMapPropertyCustom, String>() {
    override fun getMap(thisRef: TestMapPropertyCustom) = thisRef.map
    override fun getKey(desc: PropertyMetadata) = "${desc.name}Desc"
}
val mapVar = object : MapVar<TestMapPropertyCustom, String>() {
    override fun getMap(thisRef: TestMapPropertyCustom) = thisRef.map
    override fun getKey(desc: PropertyMetadata) = "${desc.name}Desc"
}

class TestMapPropertyCustom(): WithBox {
    val map = hashMapOf("aDesc" to "a", "bDesc" to "b")
    val a by mapVal
    var b by mapVar

    override fun box(): String {
        b = "newB"
        if (a != "a") return "fail at 'a'"
        if (b != "newB") return "fail at 'b' after set"
        return "OK"
    }
}

val mapValWithDefault = object : MapVal<TestMapPropertyCustomWithDefault, String>() {
    override fun getMap(thisRef: TestMapPropertyCustomWithDefault) = thisRef.map
    override fun getKey(desc: PropertyMetadata) = desc.name

    override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = "default"
}

val mapVarWithDefault = object : MapVar<TestMapPropertyCustomWithDefault, String>() {
    override fun getMap(thisRef: TestMapPropertyCustomWithDefault) = thisRef.map
    override fun getKey(desc: PropertyMetadata) = desc.name

    override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = "default"
}

class TestMapPropertyCustomWithDefault(): WithBox {
    val map = hashMapOf<String, String>()
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