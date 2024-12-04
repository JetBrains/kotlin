// MODULE_KIND: COMMON_JS

// FILE: a.kt
package foo

@JsModule("foo1")
external class A(x: Int) {
    val x: Int
}

@JsModule("foo2")
external fun func(): String

@JsModule("foo3")
external val globalVal: String

// FILE: b.kt
package bar

@JsModule("bar1")
external class A(x: Int) {
    val x: Int
}

@JsModule("bar2")
external fun func(): String

@JsModule("bar3")
external val globalVal: String

// FILE: main.kt
import foo.A
import bar.A as B
import foo.func as func1
import bar.func as func2
import foo.globalVal as globalVal1
import bar.globalVal as globalVal2

fun box(): String {
    val a = A(37)
    val b = B(73)
    assertEquals(37, a.x)
    assertEquals(73, b.x)

    val func1 = func1()
    val func2 = func2()
    assertEquals(38, func1)
    assertEquals(83, func2)

    assertEquals(39, globalVal1)
    assertEquals(93, globalVal2)

    return "OK"
}