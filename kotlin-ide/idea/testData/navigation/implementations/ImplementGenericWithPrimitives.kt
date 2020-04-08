open class Foo<T, U, Z> {
    open fun <caret>bar(t: T, u: U): Z {
        return null
    }
}

class AllTypes() : Foo<Int, Double, Int>() {
    override fun bar(i: Int, d: Double): Int = 42
}

class SomeParameters() : Foo<Double, Any, Int>() {
    override fun bar(d: Double, a: Any): Int = 42
}

class ReturnType() : Foo<Any, String, Byte>() {
    override fun bar(a: Any, s: String): Byte = 12.toByte()
}

class ParameterStillGeneric<Z>() : Foo<Int, Z, String>() {
    override fun bar(i: Int, z: Z): String = ""
}

class ReturnStillGeneric<A>() : Foo<Int, Int, A>() {
    override fun bar(i: Int, i2: Int): A = null
}

open class ThroughProxyAllGenerics<A, B, C>: Foo<A, B, C>()
class ThroughProxyAllGenericsImpl: ThroughProxyAllGenerics<Double, Int, Boolean>() {
    override fun bar(t: Double, i: Int): Boolean = true
}

open class ThroughProxySomeGenerics<A, C>: Foo<A, Int, C>()
class ThroughProxySomeGenericsImpl: ThroughProxySomeGenerics<Double, Boolean>() {
    override fun bar(t: Double, i: Int): Boolean = true
}

class NoPrimitives: Foo<String, Any, String>() {
    override fun bar(t: String, u: Any): String = ""
}

class NoOverride: Foo<String, Any, String>() {
    // Error: Not override
    override fun bar(t: Any, u: String): String = ""
}

// REF: "(in AllTypes).bar(Int, Double)"
// REF: "(in NoPrimitives).bar(String, Any)"
// REF: "(in ParameterStillGeneric).bar(Int, Z)"
// REF: "(in ReturnStillGeneric).bar(Int, Int)"
// REF: "(in ReturnType).bar(Any, String)"
// REF: "(in SomeParameters).bar(Double, Any)"
// REF: "(in ThroughProxyAllGenericsImpl).bar(Double, Int)"
// REF: "(in ThroughProxySomeGenericsImpl).bar(Double, Int)"
