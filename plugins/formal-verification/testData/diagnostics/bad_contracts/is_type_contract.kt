import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>unverifiableTypeCheck<!>(x: Int?): Boolean {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returns() implies (x is Unit)<!>
    }
    return x is String
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>nullableNotNonNullable<!>(x: Int?) {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returns() implies (x is Int)<!>
    }
}
