package test

import dependency.*

class A

class B {
    var a b<caret>y A()
}

// MULTIRESOLVE
// REF: (for T in dependency).getValue(R, kotlin.reflect.KProperty<*>)
// REF: (for T in dependency).setValue(R, kotlin.reflect.KProperty<*>, kotlin.Int)
