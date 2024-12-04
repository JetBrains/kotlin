// MODULE: lib
// FILE: TestDemo1.kt
package TestDemo1

abstract class Class {
    open val ok = "OK"

    @JsName("okFun")
    fun ok() = ok
}

// FILE: TestDemo2.kt
// MODULE: main(lib)
package TestDemo2

import TestDemo1.Class

class MyClass : Class()

fun box() = MyClass().ok()
