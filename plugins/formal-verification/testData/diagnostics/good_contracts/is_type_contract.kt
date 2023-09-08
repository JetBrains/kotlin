@file:Suppress("USELESS_IS_CHECK")

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>isString<!>(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

@OptIn(ExperimentalContracts::class)
fun Any.<!VIPER_TEXT!>isString<!>(): Boolean {
    contract {
        returns(true) implies (this@isString is String)
    }
    return this is String
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>subtypeTransitive<!>(x: Unit) {
    contract {
        returns() implies (x is Any?)
    }
}

open class Foo() {
    val bar = Bar()
}

class Bar() : Foo()

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>constructorReturnType<!>(): Boolean {
    contract {
        returns(true)
    }
    return Foo() is Foo
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>subtypeSuperType<!>(bar: Bar) {
    contract {
        returns() implies (bar is Foo)
    }
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>typeOfField<!>(foo: Foo): Boolean {
    contract {
        returns(true)
    }
    if (foo.bar is Bar) {
        return true
    } else {
        return false
    }
}

