// TARGET_BACKEND: JS_IR

package foo

class A {
    var ok: String

    init {
        var ok = "fail"
        ok = js(
            """
            ok = 'OK'
            return ok
            """
        )
        this.ok = ok
    }
}

fun box(): String {
    return A().ok
}