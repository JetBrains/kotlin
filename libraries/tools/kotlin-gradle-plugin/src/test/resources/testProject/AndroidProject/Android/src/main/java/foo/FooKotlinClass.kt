package foo

import bar.*

class FooKotlinClass {
    fun f() {
        FooJavaClass()
        BarJavaClass()
        BarKotlinClass()
    }
}