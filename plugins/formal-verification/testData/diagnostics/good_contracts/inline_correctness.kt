import org.jetbrains.kotlin.formver.plugin.NeverConvert
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@NeverConvert
inline fun not(f: () -> Boolean): Boolean = !f()

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>simple_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not { false }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>unnamed_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not {
        return true
        true
    }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>named_local_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not {
        return@not false
        true
    }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>named_nonlocal_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not {
        return@named_nonlocal_return true
        true
    }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>double_nonlocal_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not {
        val x = not {
            return true
            false
        }
        return false
        x
    }
}

// Not using run since we don't want this test to depend on it.
@NeverConvert
inline fun step(f: () -> Unit): Unit = f()

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>named_double_nonlocal_return<!>(): Boolean {
    contract {
        returns(true)
    }
    return not {
        step {
            return@not false
            return false
        }
        return false
        true
    }
}
