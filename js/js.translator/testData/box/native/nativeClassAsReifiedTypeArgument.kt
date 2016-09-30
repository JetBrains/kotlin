//FILE: nativeClassAsReifiedTypeArgument.kt

var global = ""

inline fun <reified T> log(x: T) {
    global += jsClass<T>().name + ": " + x
}

@native class C {
    override fun toString() = noImpl
}

fun box(): String {
    log(C())
    if (global != "C: C instance") return "fail: $global"
    return "OK"
}

//FILE: nativeClassAsReifiedTypeArgument.js

function C() {
}
C.prototype.toString = function() {
    return "C instance"
}