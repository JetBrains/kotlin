package test

open class C<T>(v: T) {
    operator fun getValue(p1: Any?, p2: Any?): T = v
}

class A {
    @Suppress("UNRESOLVED_REFERENCE")
    val x by lazy { Unresolved }
    val z by C<String>("z")
    val y by object: C<String>("y") {}
    val a by lazy { C<String>("a") }
    val b by lazy { object: C<String>("b") {} }
    val c by ::a
}
