// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

class Foo {
    @NeverConvert
    fun f() {}
    val x = 0
}

fun <!VIPER_TEXT!>testSafeCall<!>(foo: Foo?) = foo?.f()

@Suppress("UNNECESSARY_SAFE_CALL")
fun <!VIPER_TEXT!>testSafeCallNonNullable<!>(foo: Foo) = foo?.f()

fun <!VIPER_TEXT!>testSafeCallProperty<!>(foo: Foo?): Int? = foo?.x

@Suppress("UNNECESSARY_SAFE_CALL")
fun <!VIPER_TEXT!>testSafeCallPropertyNonNullable<!>(foo: Foo): Int? = foo?.x
