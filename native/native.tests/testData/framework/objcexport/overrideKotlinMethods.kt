package overrideKotlinMethods

import kotlin.test.*

internal interface OverrideKotlinMethods0<T> {
    fun one(): T
}

internal interface OverrideKotlinMethods1<T> : OverrideKotlinMethods0<T>

interface OverrideKotlinMethods2 {
    fun one(): Int
}

open class OverrideKotlinMethods3 {
    internal open fun one(): Number = 3
}

open class OverrideKotlinMethods4 : OverrideKotlinMethods3(), OverrideKotlinMethods1<Int>, OverrideKotlinMethods2 {
    override fun one(): Int = 2
}

interface OverrideKotlinMethods5 {
    fun one(): Int
}

interface OverrideKotlinMethods6 : OverrideKotlinMethods5

// Using `Any` because Kotlin forbids internal type in public function signature.
@Throws(Throwable::class)
fun test0(obj: Any) {
    val obj0 = obj as OverrideKotlinMethods0<*>
    assertEquals(1, obj0.one())
}

// Using `Any` because Kotlin forbids internal type in public function signature.
@Throws(Throwable::class)
fun test1(obj: Any) {
    val obj1 = obj as OverrideKotlinMethods1<*>
    assertEquals(1, obj1.one())
}

@Throws(Throwable::class)
fun test2(obj: OverrideKotlinMethods2) {
    assertEquals(1, obj.one())
}

@Throws(Throwable::class)
fun test3(obj: OverrideKotlinMethods3) {
    assertEquals(1, obj.one())
}

@Throws(Throwable::class)
fun test4(obj: OverrideKotlinMethods4) {
    assertEquals(1, obj.one())
}

@Throws(Throwable::class)
fun test5(obj: OverrideKotlinMethods5) {
    assertEquals(1, obj.one())
}

@Throws(Throwable::class)
fun test6(obj: OverrideKotlinMethods6) {
    assertEquals(1, obj.one())
}
