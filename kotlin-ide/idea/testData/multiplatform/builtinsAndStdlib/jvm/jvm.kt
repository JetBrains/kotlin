// !DIAGNOSTICS: -UNUSED_PARAMETER -NO_REFLECTION_IN_CLASS_PATH

package mpp

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Cloneable<!>

fun foo(x: KAnnotatedElement): Boolean = true

class Foo {
    fun bar(a: Int, b: Int): KCallable<*> { TODO() }
}

fun jvmFun() {
}

fun getKCallable(): KCallable<*> = ::jvmFun

fun <!LINE_MARKER!>main<!>() {
    val ref = ::jvmFun
    val typedRef: KCallable<*> = getKCallable()
    ref.call()
    typedRef.call()
    foo(Foo::bar)
}

