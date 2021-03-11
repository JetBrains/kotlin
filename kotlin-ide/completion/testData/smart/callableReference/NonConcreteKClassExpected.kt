import kotlin.reflect.KClass

interface I

fun foo(kClass: KClass<out I>){}
fun foo(kClass: KClass<in I>, p: Int){}
fun foo(kClass: KClass<*>, c: Char){}
fun <T : Any> foo(kClass: KClass<T>, d: Double){}

fun bar() {
    foo(<caret>)
}

// ABSENT: "I::class"
// ABSENT: "Any::class"
// ABSENT: "T::class"
