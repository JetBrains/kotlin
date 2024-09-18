// TARGET_BACKEND: JS

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