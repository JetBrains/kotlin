package bar

import foo.*

class BarKotlinClass {
    fun f() {
        BarJavaClass()
        FooJavaClass()
        FooKotlinClass()
    }
}