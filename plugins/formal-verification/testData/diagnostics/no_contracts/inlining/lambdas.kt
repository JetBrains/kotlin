// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

fun <!VIPER_TEXT!>explicitArg<!>(): Int {
    return invoke { x -> x + x }
}

fun <!VIPER_TEXT!>implicitArg<!>(): Int {
    return invoke { it * 2 }
}

fun <!VIPER_TEXT!>lambdaIf<!>(): Int {
    return invoke {
        if (it == 0) {
            it + 1
        } else {
            it + 2
        }
    }
}

fun <!VIPER_TEXT!>returnValueNotUsed<!>(): Unit {
    invoke { it }
}

fun <!VIPER_TEXT!>shadowing<!>(): Int {
    val x = 1
    val y = 1
    return invoke { x ->
        val y = 0
        x + y
    }
}

@NeverConvert
@Suppress("NOTHING_TO_INLINE")
inline fun nestedHelper(): Int {
    val x = 2
    return invoke { x -> x + 1 }
}

fun <!VIPER_TEXT!>nested<!>(): Int {
    val x = 2
    return nestedHelper()
}

@NeverConvert
inline fun passthroughHelper(f: (Int) -> Int) {
    invoke(f)
}

fun <!VIPER_TEXT!>lambdaPassthrough<!>() {
    passthroughHelper { it + 1 }
}