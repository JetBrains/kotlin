import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun returns_boolean(): Boolean {
    return false
}

fun <!VIPER_TEXT!>dynamic_lambda_invariant<!>(f: () -> Int) {
    while (returns_boolean()) {
        f()
    }
}

fun <!VIPER_TEXT!>function_assignment<!>(f: () -> Int) {
    val g = f
    while (returns_boolean()) {
        g()
    }
}

fun <!VIPER_TEXT!>conditional_function_assignment<!>(b: Boolean, f: () -> Int, h: () -> Int) {
    val g = if (b) f else h
    while (returns_boolean()) {
        g()
    }
}