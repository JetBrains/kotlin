// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// KJS_WITH_FULL_RUNTIME

// FILE: annotations.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated3(val kClass: KClass<*>)

// FILE: foo.kt
@Associated1(Bar::class)
@Associated2(Baz::class)
class Foo

// FILE: bar.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

object Bar

// FILE: baz.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated2(val kClass: KClass<*>)

object Baz

private class C(var list: List<String>?)

private interface I1 {
    fun foo(): Int
    fun bar(c: C)
}

private object I1Impl : I1 {
    override fun foo() = 42
    override fun bar(c: C) {
        c.list = mutableListOf("zzz")
    }
}

@Associated1(I1Impl::class)
private class I1ImplHolder

private interface I2 {
    fun foo(): Int
}

private object I2Impl : I2 {
    override fun foo() = 17
}

@Associated1(I2Impl::class)
private class I2ImplHolder

@Associated2(A.Companion::class)
class A {
    companion object : I2 {
        override fun foo() = 20
    }
}

@OptIn(ExperimentalAssociatedObjects::class)
fun KClass<*>.getAssociatedObjectByAssociated2(): Any? {
    return this.findAssociatedObject<Associated2>()
}

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {

    if (Foo::class.findAssociatedObject<Associated1>() != Bar) return "fail 1"

    if (Foo::class.findAssociatedObject<Associated2>() != Baz) return "fail 2"

    if (Foo::class.findAssociatedObject<Associated3>() != null) return "fail 3"

    if (Bar::class.findAssociatedObject<Associated1>() != null) return "fail 4"

    val i1 = I1ImplHolder::class.findAssociatedObject<Associated1>() as I1
    if (i1.foo() != 42) return "fail 5"

    val c = C(null)
    i1.bar(c)
    if (c.list!![0] != "zzz") return "fail 6"

    val i2 = I2ImplHolder()::class.findAssociatedObject<Associated1>() as I2
    if (i2.foo() != 17) return "fail 7"

    val a = A::class.findAssociatedObject<Associated2>() as I2
    if (a.foo() != 20) return "fail 8"

    if (Foo::class.getAssociatedObjectByAssociated2() != Baz) return "fail 9"

    if ((A::class.getAssociatedObjectByAssociated2() as I2).foo() != 20) return "fail 10"

    if (Int::class.findAssociatedObject<Associated1>() != null) return "fail 11"

    if (10::class.findAssociatedObject<Associated2>() != null) return "fail 12"

    return "OK"
}