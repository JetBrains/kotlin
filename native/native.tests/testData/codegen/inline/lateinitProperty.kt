// KT-64511: lateinit is not lowered with caches
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// MODULE: lib
// FILE: lib.kt

package a

fun IntArray.forEachNoInline(block: (Int) -> Unit) = this.forEach { block(it) }

inline fun foo(values: IntArray, crossinline block: (Int, Int, Int) -> Int): Int {
    val o = object {
        lateinit var s: String
        var x: Int = 42
    }
    values.forEachNoInline {
        o.x = block(o.x, o.s.length, it)
    }
    return o.x
}

// MODULE: main(lib)
// FILE: main.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import a.*
import kotlin.test.*

fun box(): String {
    assertFailsWith<UninitializedPropertyAccessException> {
        foo(intArrayOf(1, 2, 3)) { x, y, z -> x + y - z }
    }
    return "OK"
}