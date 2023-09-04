import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
