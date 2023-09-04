import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>unknown<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, UNKNOWN)
    }
    return f(1)
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>at_most_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, AT_MOST_ONCE)
    }
    return f(1)
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_at_most_once<!>(f : (Int) -> Int) : Int{
    <!WRONG_INVOCATION_KIND!>contract {
        callsInPlace(f, AT_MOST_ONCE)
    }<!>
    return f(at_most_once(f))
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>exactly_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, EXACTLY_ONCE)
    }
    return f(1)
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_exactly_once<!>(f : (Int) -> Int) : Int{
    <!WRONG_INVOCATION_KIND!>contract {
        callsInPlace(f, EXACTLY_ONCE)
    }<!>
    return f(f(1))
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>at_least_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, AT_LEAST_ONCE)
    }
    return exactly_once(f)
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_at_least_once<!>(f : (Int) -> Int) : Int{
    <!WRONG_INVOCATION_KIND!>contract {
        callsInPlace(f, AT_LEAST_ONCE)
    }<!>
    return unknown(f)
}

@OptIn(ExperimentalContracts::class)
inline fun <!VIPER_TEXT!>loopWhile<!>(lambda: () -> Boolean)
{
    contract {
        callsInPlace(lambda, AT_LEAST_ONCE)
    }
    while (lambda()) { /* no body */ }
}

@OptIn(ExperimentalContracts::class)
inline fun <!VIPER_TEXT!>loopUntil<!>(lambda: () -> Boolean)
{
    contract {
        callsInPlace(lambda, AT_LEAST_ONCE)
    }
    while (!lambda()) { /* no body */ }
}