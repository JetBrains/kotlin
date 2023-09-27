import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@NeverConvert
fun escape(f : () -> Unit) {
    // No contract means that we assume this function may leak the function object passed to it.
    f()
}

@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>invalid_calls_in_place<!>(f : () -> Unit) {
    contract {
        callsInPlace(f)
    }
    escape(f)
}

@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>function_object_call<!>(f: (() -> Unit) -> Unit, g: () -> Unit) {
    contract {
        callsInPlace(g)
    }
    return f(g)
}