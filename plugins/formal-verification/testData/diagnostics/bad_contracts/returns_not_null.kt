import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>returns_null<!>(): Int? {
    contract {
        <!UNEXPECTED_RETURNED_VALUE!>returnsNotNull()<!>
    }
    return null
}