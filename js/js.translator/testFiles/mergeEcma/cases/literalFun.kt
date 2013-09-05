package foo

fun ok(f: (s: String) -> String): String {
    return f("O");
}

fun box(): String {
    return ok { (s) -> s + "K" }
}