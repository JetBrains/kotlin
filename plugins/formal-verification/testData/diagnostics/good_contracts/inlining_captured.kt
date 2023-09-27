import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!VIPER_TEXT!>single_call<!>(g: (Int) -> Int): Int {
    contract {
        callsInPlace(g, EXACTLY_ONCE)
    }
    return invoke { g(it) }
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!VIPER_TEXT!>double_call<!>(f: (Int) -> Int, g: (Int) -> Int): Int {
    contract {
        callsInPlace(f, EXACTLY_ONCE)
        callsInPlace(g, EXACTLY_ONCE)
    }
    return invoke { g(it); f(it) }
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!VIPER_TEXT!>nested_call<!>(f: (Int) -> Int, g: (Int) -> Int): Int {
    contract {
        callsInPlace(f, EXACTLY_ONCE)
        callsInPlace(g, EXACTLY_ONCE)
    }
    return invoke { f(g(it)) }
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!VIPER_TEXT!>non_local_return<!>(g: () -> Unit): Int {
    contract {
        callsInPlace(g, EXACTLY_ONCE)
    }
    return invoke {
        g()
        return@invoke 1
        g()
        it
    }
}