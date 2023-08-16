import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>returns_true<!>(): Boolean {
    contract {
        returns()
        returns(true)
    }
    return true
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>returns_false<!>(): Boolean {
    contract {
        returns()
        returns(false)
    }
    return false
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>conditional_basic<!>(b: Boolean): Boolean {
    contract {
        returns(true) implies (true)
        returns(false) implies (b)
    }
    return true
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>binary_logic_expressions<!>(a:Boolean, b: Boolean): Boolean {
    contract {
        returns(false) implies (b && false)
        returns(true) implies ((true || a) && (b || true))
    }
    return true
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>logical_not<!>(b: Boolean): Boolean {
    contract{
        returns(true) implies (!b && b)
        returns(false) implies (b || !b)
    }
    return false
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>call_fun_with_contracts<!>(b: Boolean): Boolean {
    contract {
        returns(true)
    }
    val a = binary_logic_expressions(b, b)
    return a
}