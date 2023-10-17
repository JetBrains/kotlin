import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@NeverVerify
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>bad_returns<!>(): Boolean {
    contract {
        returns(true)
    }
    return false
}

@NeverConvert
fun noop() {}
