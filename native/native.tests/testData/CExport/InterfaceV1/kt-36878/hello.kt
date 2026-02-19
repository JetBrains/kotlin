/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

value class ValueClass(val number: Int) {}
class UsualClass(val number: UByte) {}
class Foo<T>

object ObjectForExample {
    val nullableVal: ValueClass? = ValueClass(153)
    val usual: UsualClass = UsualClass(128.toUByte())
    val uByteArray: UByteArray = ubyteArrayOf(0x40.toUByte(), 0x80.toUByte())
    val uIntArray: UIntArray = intArrayOf(0x40, 0x80).toUIntArray()
    val fooValueClass = Foo<ValueClass>()
    val fooUsualClass = Foo<UsualClass>()
    fun fooValue(foo: ValueClass?) {
        println("ValueClass? = $foo")
    }
    fun fooUsual(foo: UsualClass?) {
        println("UsualClass? = ${foo?.number}")
    }
    fun fooUByteArray(foo: UByteArray) {
        println("UByteArray = $foo: ${foo.joinToString()}")

    }
    fun fooUIntArrayNullable(foo: UIntArray?) {
        println("UIntArray? = $foo: ${foo?.joinToString()}")
    }
    fun fooFooValue(foo: Foo<ValueClass>) {
        println("Foo<ValueClass> = ${foo::class}")
    }
    fun fooFooUsual(foo: Foo<UsualClass>) {
        println("Foo<UsualClass> = ${foo::class}")
    }
}