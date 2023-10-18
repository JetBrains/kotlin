import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO: currently the effect on contracts are not annotated with `VIPER_VERIFICATION_ERROR`.
// That's because the source information is embedded on the use-site of the passed lambda function.
// This will be fixed in the next pull-request.

@NeverConvert
fun escape(f : () -> Unit) {
    // No contract means that we assume this function may leak the function object passed to it.
    f()
}

@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>invalid_calls_in_place<!>(f : () -> Unit) {
    contract {
        callsInPlace(f)
    }
    <!VIPER_VERIFICATION_ERROR!>escape(f)<!>
}

@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>function_object_call<!>(f: (() -> Unit) -> Unit, g: () -> Unit) {
    contract {
        callsInPlace(g)
    }
    return <!VIPER_VERIFICATION_ERROR!>f(g)<!>
}
