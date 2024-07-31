// EXPECTED_REACHABLE_NODES: 1301

public interface A {
    @JsName("foo")
    fun foo() {
    }
}
public interface B : A {
    @JsName("boo")
    fun boo() {
    }
}

external class Function(args: String, body: String)

val hasProp = Function("obj, prop", "return obj[prop] !== undefined") as ((Any, String) -> Boolean)

fun box(): String {
    val a = object: A {
    }
    val b = object: B {
    }

    if (!hasProp(a, "foo")) return "A hasn't foo"
    if (hasProp(a, "boo")) return "A has boo"

    if (!hasProp(b, "foo")) return "B hasn't foo"
    if (!hasProp(b, "boo")) return "B hasn't boo"

    return "OK"
}