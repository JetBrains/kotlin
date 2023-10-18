import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

// Included for use in other tests.
@NeverConvert
@OptIn(ExperimentalContracts::class)
fun unknown(f : (Int) -> Int) : Int{
    contract {
        callsInPlace(f, UNKNOWN)
    }
    return f(1)
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrect_at_most_once<!>(f : (Int) -> Int) : Int{
    contract {
        <!VIPER_VERIFICATION_ERROR!>callsInPlace(f, AT_MOST_ONCE)<!>
    }
    return f(unknown(f))
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrect_exactly_once<!>(f : (Int) -> Int) : Int{
    contract {
        <!VIPER_VERIFICATION_ERROR!>callsInPlace(f, EXACTLY_ONCE)<!>
    }
    return f(f(1))
}

@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrect_at_least_once<!>(f : (Int) -> Int) : Int{
    contract {
        <!VIPER_VERIFICATION_ERROR!>callsInPlace(f, AT_LEAST_ONCE)<!>
    }
    return unknown(f)
}

// Consistency check with current Kotlin contract verification mechanism.
@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrect_exactly_once_with_catch<!>(f : () -> Unit) : Unit {
    contract {
        <!VIPER_VERIFICATION_ERROR!>callsInPlace(f, EXACTLY_ONCE)<!>
    }
    try {
        f()
    } catch (e: Exception) {
    }
}

// Consistency check with current Kotlin contract verification mechanism.
@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrect_exactly_once_with_call_in_catch<!>(f : () -> Unit) : Unit {
    contract {
        <!VIPER_VERIFICATION_ERROR!>callsInPlace(f, EXACTLY_ONCE)<!>
    }
    try {
        f()
    } catch (e: Exception) {
        f()
    }
}
