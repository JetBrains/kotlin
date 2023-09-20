import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>nested_call<!>(g: (Int) -> Int): Int {
    contract {
        callsInPlace(g, EXACTLY_ONCE)
    }
    return invoke { g(g(it)) }
}

@OptIn(ExperimentalContracts::class)
@Suppress("WRONG_INVOCATION_KIND", "LEAKED_IN_PLACE_LAMBDA")
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>double_call<!>(g: (Int) -> Int): Int {
    contract {
        callsInPlace(g, AT_MOST_ONCE)
    }
    return invoke { g(it); g(it) }
}