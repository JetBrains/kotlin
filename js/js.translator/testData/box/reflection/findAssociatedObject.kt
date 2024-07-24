// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated2(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated3(val kClass: KClass<*>)

@Associated1(Bar::class)
@Associated2(Baz::class)
class Foo

object Bar
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

@Associated1(I1Impl::class)
private interface I1ImplInterfaceHolder

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

    val i3 = I1ImplInterfaceHolder::class.findAssociatedObject<Associated1>() as I1
    if (i3.foo() != 42) return "fail 13"

    return "OK"
}