package testing.kotlin

import testing.JavaClass

class KotlinClass: JavaClass() {
    override fun bar(): String = "Override"
}

fun usages() {
    val a = JavaClass().bar()
    val b = KotlinClass().bar()
}