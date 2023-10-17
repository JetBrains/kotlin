import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT!>returns_null_unverifiable<!>(x: Int?): Int? {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returns() implies false<!>
    }
    return null
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT!>non_nullable_returns_null<!>(x: Int): Int {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returns(null)<!>
    }
    return x
}
