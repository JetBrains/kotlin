import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>isString<!>(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

@OptIn(ExperimentalContracts::class)
fun Any.<!VIPER_TEXT!>isString<!>(): Boolean {
    contract {
        returns(true) implies (this@isString is String)
    }
    return this is String
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>subtypeTransitive<!>(x: Unit) {
    contract {
        // Kotlin knows that this check will always succeed and marks it as useless, however, we still want to test that
        // Viper can prove this as well.
        returns() implies (<!USELESS_IS_CHECK!>x is Any?<!>)
    }
}

open class Foo()

class Bar() : Foo()

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>subtypeSuperType<!>(bar: Bar) {
    contract {
        // Kotlin knows that this check will always succeed and marks it as useless, however, we still want to test that
        // Viper can prove this as well.
        returns() implies (<!USELESS_IS_CHECK!>bar is Foo<!>)
    }
}