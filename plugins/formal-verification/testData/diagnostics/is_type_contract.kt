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
fun <!VIPER_TEXT!>subtypeTransitive<!>(x: Unit) {
    contract {
        // Kotlin knows that this check will always succeed and marks it as useless, however, we still want to test that
        // Viper can prove this as well.
        returns() implies (<!USELESS_IS_CHECK!>x is Any?<!>)
    }
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>unverifiableTypeCheck<!>(x: Int?): Boolean {
    contract {
        returns() implies (x is Unit)
    }
    return x is String
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>nullableNotNonNullable<!>(x: Int?) {
    contract {
        returns() implies (x is Int)
    }
}