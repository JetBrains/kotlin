import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

fun <!VIPER_TEXT!>capture_arg<!>(g: (Int) -> Int): Int {
    return invoke { g(it) }
}

fun <!VIPER_TEXT!>capture_var<!>(): Int {
    val x = 1
    return invoke { it + x }
}

fun <!VIPER_TEXT!>capture_and_shadow<!>(x: Int): Int {
    return invoke {
        val y = x
        val x = 1
        it + x + y
    }
}

@NeverConvert
inline fun invoke_clash(f: (Int) -> Int): Int {
    val x = 1
    return f(0) + x
}

fun <!VIPER_TEXT!>capture_var_clash<!>(x: Int): Int {
    return invoke_clash { it * x }
}

fun <!VIPER_TEXT!>capture_and_shadow_clash<!>(x: Int): Int {
    return invoke_clash {
        val y = x
        val x = 2
        x + y + it
    }
}

fun <!VIPER_TEXT!>nested_lambda_shadowing<!>(x: Int): Int {
    return invoke_clash {
        invoke_clash {
            val x = 3
            x + it
        }
        val y = x
        val x = 4
        x + y + it
    }
}

@NeverConvert
inline fun double_invoke(f: (Int) -> Int): Int {
    f(0)
    return f(1)
}

fun <!VIPER_TEXT!>call_double_invoke<!>(x: Int): Int {
    return double_invoke {
        val x = it
        x
    }
}