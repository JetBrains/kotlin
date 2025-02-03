package org.jetbrains

@Target(allowedTargets = [AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER])
annotation class A

class Outer {
    class Inner
}

fun foo(x: @A List<@A String>, y: @A Outer.Inner, z: @A Int): @A String = ""