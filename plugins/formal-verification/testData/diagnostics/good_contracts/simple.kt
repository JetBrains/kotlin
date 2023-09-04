import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

fun <!VIPER_TEXT!>without_contract<!>() {}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>with_contract<!>() {
    contract() {
        returns()
    }
}
