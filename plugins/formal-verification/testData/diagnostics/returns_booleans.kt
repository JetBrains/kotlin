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
fun <!VIPER_TEXT!>conditional<!>(a: Boolean, b: Boolean): Boolean {
    contract {
        returns(true) implies (true)
        returns(false) implies (b && false)
        returns(true) implies ((true || a) && (b || true))
    }
    return true
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>call_fun_with_contracts<!>(b: Boolean): Boolean {
    contract {
        returns(true)
    }
    val a = conditional(b, b)
    return a
}