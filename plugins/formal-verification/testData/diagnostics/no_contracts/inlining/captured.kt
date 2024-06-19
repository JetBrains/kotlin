// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

fun <!VIPER_TEXT!>captureArg<!>(g: (Int) -> Int): Int {
    return invoke { g(it) }
}

fun <!VIPER_TEXT!>captureVar<!>(): Int {
    val x = 1
    return invoke { it + x }
}

fun <!VIPER_TEXT!>captureAndShadow<!>(x: Int): Int {
    return invoke {
        val y = x
        val x = 1
        it + x + y
    }
}

@NeverConvert
inline fun invokeClash(f: (Int) -> Int): Int {
    val x = 1
    return f(0) + x
}

fun <!VIPER_TEXT!>captureVarClash<!>(x: Int): Int {
    return invokeClash { it * x }
}

fun <!VIPER_TEXT!>captureAndShadowClash<!>(x: Int): Int {
    return invokeClash {
        val y = x
        val x = 2
        x + y + it
    }
}

fun <!VIPER_TEXT!>nestedLambdaShadowing<!>(x: Int): Int {
    return invokeClash {
        invokeClash {
            val x = 3
            x + it
        }
        val y = x
        val x = 4
        x + y + it
    }
}

@NeverConvert
inline fun doubleInvoke(f: (Int) -> Int): Int {
    f(0)
    return f(1)
}

fun <!VIPER_TEXT!>callDoubleInvoke<!>(x: Int): Int {
    return doubleInvoke {
        val x = it
        x
    }
}