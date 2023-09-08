import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

// Included for use in other tests.
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>unknown<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, UNKNOWN)
    }
    return f(1)
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_at_most_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, AT_MOST_ONCE)
    }
    return f(unknown(f))
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_exactly_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, EXACTLY_ONCE)
    }
    return f(f(1))
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>incorrect_at_least_once<!>(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, AT_LEAST_ONCE)
    }
    return unknown(f)
}
