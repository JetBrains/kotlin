import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class IntHolder(val x: Int)

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>getX<!>(a: Any): Int? {
    contract {
        <!VIPER_VERIFICATION_ERROR!>returnsNotNull() implies (a !is IntHolder)<!>
    }
    return (a as? IntHolder)?.x
}
