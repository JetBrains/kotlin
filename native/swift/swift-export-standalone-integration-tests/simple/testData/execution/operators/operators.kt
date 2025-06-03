// KIND: STANDALONE
// MODULE: Operators
// FILE: operators.kt

package operators

import kotlin.reflect.KProperty

data class Foo(var value: Int = 0) {
    // Iterators
    object EmptyIterator: Iterator<Int> {
        override operator fun next(): Int = 0
        override operator fun hasNext(): Boolean = false
    }

    operator fun iterator(): Iterator<Int> = EmptyIterator

    // Subscripting

    operator fun get(index: Int): Int = index
    operator fun set(index: Int, value: Int) {}

    // Unary
    operator fun not(): Foo = this
    operator fun unaryPlus(): Foo = this
    operator fun unaryMinus(): Foo = this

    // Unary
    operator fun inc(): Foo = this
    operator fun dec(): Foo = this

    // Binary
    operator fun plus(other: Foo): Foo = this // +
    operator fun minus(other: Foo): Foo = this // -
    operator fun times(other: Foo): Foo = this // *
    operator fun div(other: Foo): Foo = this // /
    operator fun rem(other: Foo): Foo = this // %

    operator fun rangeTo(other: Foo): Foo = this // ..
    operator fun rangeUntil(other: Foo): Foo = this // ..<

    // Binary-words
    operator fun contains(other: Foo): Boolean = true

    // Compount-assignment

    operator fun plusAssign(other: Foo): Unit {} // +=
    operator fun minusAssign(other: Foo): Unit {} // -=
    operator fun timesAssign(other: Foo): Unit {} // *=
    operator fun divAssign(other: Foo): Unit {} // /=
    operator fun remAssign(other: Foo): Unit {} // %=

    // Equality & Comparsion

    override operator fun equals(other: Any?): Boolean = this.value == (other as? Foo)?.value
    operator fun compareTo(other: Foo): Int = this.value - other.value

    // Delegation

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = this.value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) { this.value = value }
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): Foo = this

    // function call

    operator fun invoke(): Foo = this
}