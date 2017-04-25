// EXPECTED_REACHABLE_NODES: 508
package foo

public interface A {
    fun foo() {
    }
}
public interface B : A {
    fun boo() {
    }
}

external class Function(vararg args: String)

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

    val PREFIX = "_.foo"
    if (eval("$PREFIX.A") == null) return "$PREFIX.A not found"
    if (eval("$PREFIX.B") == null) return "$PREFIX.B not found"
    if (eval("$PREFIX.A === $PREFIX.B") as Boolean) return "A and B refer to the same object"

    return "OK"
}