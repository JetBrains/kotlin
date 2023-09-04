import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

inline fun <!VIPER_TEXT!>invoke<!>(f: (Int) -> Int): Int {
    val x = f(0)
    return f(0)
}

inline fun <!VIPER_TEXT!>invoke_with_int<!>(f: (Int) -> Int, n: Int): Int {
    val x = n + n
    return f(x)
}

@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>call_invoke<!>(g: (Int) -> Int): Int {
    contract {
        callsInPlace(g, AT_LEAST_ONCE)
    }
    val z = invoke(g)
    return invoke(g)
}

@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>call_invoke_with_int<!>(g: (Int) -> Int): Int {
    contract {
        callsInPlace(g, AT_LEAST_ONCE)
    }
    val z = invoke_with_int(g, 1)
    return invoke_with_int(g, g(1))
}

@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>name_clashes<!>(f: (Int) -> Int): Int {
    contract {
        callsInPlace(f, AT_LEAST_ONCE)
    }
    val x = invoke(f)
    return invoke_with_int(f, x)
}

@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>different_return_type<!>(f: (Int) -> Int): Boolean {
    contract {
        callsInPlace(f, AT_LEAST_ONCE)
    }
    val x = invoke(f)
    return x == invoke(f)
}