import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>simple_returns_null<!>(x: Int?): Int? {
    contract {
        returns(null)
        returnsNotNull() implies true
    }
    return null
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>returns_null_implies<!>(x: Boolean?): Boolean? {
    contract {
        returns(null) implies (x == null)
        returnsNotNull() implies (x != null)
    }
    return x
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>returns_null_with_if<!>(x: Int?, y: Int?, z: Int?): Int? {
    contract {
        returns(null) implies ((x == null && y == null) || z == null)
        returnsNotNull() implies (x != null || y != null)
    }
    if (x == null){
        return y
    } else {
        return z
    }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>non_nullable_returns_not_null<!>(x: Int): Int {
    contract {
        returnsNotNull()
    }
    return x
}

@OptIn(ExperimentalContracts::class)
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>non_nullable_returns_null<!>(x: Int): Int {
    contract {
        returns(null)
    }
    return x
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>non_nullable_compared_to_null<!>(x: Int, y: Int): Int {
    contract {
        returns() implies (<!SENSELESS_COMPARISON!>y == null<!> || <!SENSELESS_COMPARISON!>x != null<!>)
    }
    return x
}
