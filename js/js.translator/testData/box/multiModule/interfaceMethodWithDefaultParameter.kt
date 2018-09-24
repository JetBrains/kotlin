// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1289
// MODULE: lib
// FILE: lib.kt
public interface I {
    public fun f(p: String = "O"): String
}

// MODULE: main(lib)
// FILE: main.kt
public class C : I {
    override fun f(p: String) = p + "K"
}

fun box() = C().f()