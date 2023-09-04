import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>returns_null_unverifiable<!>(x: Int?): Int? {
    contract {
        returns() implies false
    }
    return null
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>non_nullable_returns_null<!>(x: Int): Int {
    contract {
        returns(null)
    }
    return x
}
