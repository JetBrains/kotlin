import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

fun without_contract() {}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT!>with_contract<!>() {
    contract() {
        returns()
    }
}