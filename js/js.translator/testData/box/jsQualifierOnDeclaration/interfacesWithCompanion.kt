// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// FILE: bar.kt
package bar

@JsQualifier("bar")
external interface Bar {
    companion object {
        fun ok(): String
    }
}

// FILE: test.kt
import bar.Bar

fun box(): String {
    return Bar.ok()
}

// FILE: test.js
var bar = function() {
    var Bar = {
        ok() {
            return "OK"
        }
    };
    return {
        Bar: Bar
    }
}();
