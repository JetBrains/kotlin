// WITH_RUNTIME
@file:JvmName("QPackage")
package jvmName

@JvmName("QTopLevel")
fun topLevel() {}

class Foo {
    @JvmName("QFoo")
    fun foo() {}

    var x: Int
        @JvmName("QGetInt") get() = 0
        @JvmName("QSetInt") set(v) {}
}