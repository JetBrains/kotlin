// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

class Foo(var x: Int)

@NeverConvert
fun getFoo(): Foo = Foo(0)
@NeverConvert
fun sideEffect(): Int = 0

fun <!VIPER_TEXT!>test<!>() {
    getFoo().x = sideEffect()
    val y = getFoo().x + sideEffect()
}
