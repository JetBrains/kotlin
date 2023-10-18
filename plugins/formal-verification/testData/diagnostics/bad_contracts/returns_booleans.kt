import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>incorrectly_returns_false<!>(): Boolean {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returns(true)<!>
    }
    return false
}

