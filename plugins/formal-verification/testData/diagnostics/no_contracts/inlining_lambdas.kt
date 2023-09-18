import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

fun <!VIPER_TEXT!>explicit_arg<!>(): Int {
    return invoke { x -> x + x }
}

fun <!VIPER_TEXT!>implicit_arg<!>(): Int {
    return invoke { it * 2 }
}

fun <!VIPER_TEXT!>lambda_if<!>(): Int {
    return invoke {
        if (it == 0) {
            it + 1
        } else {
            it + 2
        }
    }
}

fun <!VIPER_TEXT!>return_value_not_used<!>(): Unit {
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

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>foo<!>(): Int {
    val x = 2
    return invoke { x -> x + 1 }
}

fun <!VIPER_TEXT!>nested<!>(): Int {
    val x = 2
    return foo()
}