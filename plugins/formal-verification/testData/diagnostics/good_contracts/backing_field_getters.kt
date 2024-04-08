import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

class Z
class Y { val z = Z() }
class X { val y = Y() }

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>cascadeGet<!>(x: X): Z {
    contract {
        returns()
    }
    return x.y.z
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>receiverNotNullProved<!>(x: X?): Boolean {
    contract {
        returns(true) implies (x != null)
    }
    return x?.y != null
}

class NullableY { val z: Z? = Z() }
class NullableX { val y: NullableY? = NullableY() }

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>cascadeNullableGet<!>(x: NullableX?): Z? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return x?.y?.z
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>cascadeNullableSmartcastGet<!>(x: NullableX?): Z? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return if (x == null) null else if (x.y == null) null else x.y.z
}

class Baz { val x: Int = 4 }

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>nullableReceiverNotNullSafeGet<!>(): Boolean {
    contract {
        returns(false)
    }
    val f: Baz? = Baz()
    return f?.x == null
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>nullableReceiverNullSafeGet<!>(): Boolean {
    contract {
        returns(true)
    }
    val f: Baz? = null
    return f?.x == null
}

@Suppress("UNNECESSARY_SAFE_CALL")
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>nonNullableReceiverSafeGet<!>(): Boolean {
    contract {
        returns(false)
    }
    val f: Baz = Baz()
    return f?.x == null
}
