// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
ArrayBuffer.isView = function isView(a) {
    isView.called = true;
    return a != null && a.__proto__ != null && a.__proto__.__proto__ === Int8Array.prototype.__proto__;
}

// FILE: main.kt
fun box(): String {
    val intArr = intArrayOf(5, 4, 3, 2, 1)
    val result = IntArray(5).apply { intArr.copyInto(this) }

    assertEquals(result.joinToString(","), intArr.joinToString(","))
    assertEquals(js("ArrayBuffer.isView.called"), true)

    return "OK"
}
