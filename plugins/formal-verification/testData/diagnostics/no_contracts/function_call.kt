import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun f(x: Int): Int = x

fun <!VIPER_TEXT!>function_call<!>() {
    f(0)
    f(0)
}

fun <!VIPER_TEXT!>function_call_nested<!>() {
    f(f(f(0)))
}
