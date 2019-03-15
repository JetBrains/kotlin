// WITH_RUNTIME

// FILE: lib/Prop.java
package lib;

public abstract class Prop<T> {
    public abstract int get(T key);
    public abstract void set(T key, int value);
}

// FILE: test.kt
package test

import lib.Prop

class Cl(var name: String)

val TEST = object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
}

val TESTS_ARRAY = arrayOf(object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
})

val TESTS_LIST = listOf(object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
})