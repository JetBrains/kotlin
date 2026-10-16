// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

open class Shape {
    open fun area(): Double = 0.0
}

class Circle(val r: Double) : Shape() {
    override fun area(): Double = 3.14159 * r * r
}

class Rectangle(val w: Double, val h: Double) : Shape() {
    override fun area(): Double = w * h
}

fun box(): String {
    val shapes: List<Shape> = listOf(Circle(2.0), Rectangle(3.0, 4.0))

    GC.collect()

    val a1 = shapes[0].area()
    val a2 = shapes[1].area()

    if (a1 < 12.5 || a1 > 12.6) return "FAIL circle area: $a1"
    if (a2 != 12.0) return "FAIL rect area: $a2"

    return "OK"
}
