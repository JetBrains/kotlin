// DUMP_IR

// FILE: data/delegate.kt
package data

class Data(val data: Int)

class Delegate(private val data: Int) {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Data {
        return Data(data)
    }
}

fun delegate(data: Int) = Delegate(data)
// FILE: test/ClassWithDelegatedProperty.kt
package test

import data.delegate

class ClassWithDelegatedProperty(data: Int) {
    var delegatedProperty by delegate(data)
}

// FILE: main.kt
package home

import test.ClassWithDelegatedProperty

fun preview(foo: ClassWithDelegatedProperty) {
}