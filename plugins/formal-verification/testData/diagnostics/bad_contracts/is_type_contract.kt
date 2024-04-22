import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Suppress("USELESS_IS_CHECK")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>unverifiableTypeCheck<!>(x: Int?): Boolean {
    contract {
        <!CONDITIONAL_EFFECT_ERROR!>returns() implies (x is Unit)<!>
    }
    return x is String
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>nullableNotNonNullable<!>(x: Int?) {
    contract {
        <!CONDITIONAL_EFFECT_ERROR!>returns() implies (x is Int)<!>
    }
}
