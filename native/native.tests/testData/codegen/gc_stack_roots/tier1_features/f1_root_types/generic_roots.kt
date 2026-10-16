// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f1_root_types

import kotlin.native.runtime.GC

class Container<T>(val item: T)

fun <T> checkContainer(c: Container<T>): T {
    GC.collect()
    return c.item
}

fun box(): String {
    val stringBox = Container("hello generic")
    val intBox = Container(12345)
    val listBox = Container(listOf("a", "b", "c"))

    if (checkContainer(stringBox) != "hello generic") return "FAIL stringBox"
    if (checkContainer(intBox) != 12345) return "FAIL intBox"
    if (checkContainer(listBox) != listOf("a", "b", "c")) return "FAIL listBox"

    return "OK"
}
