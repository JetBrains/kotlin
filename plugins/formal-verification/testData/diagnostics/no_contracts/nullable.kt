import org.jetbrains.kotlin.formver.plugin.NeverConvert

fun <!VIPER_TEXT!>use_nullable_twice<!>(x: Int?): Int? {
    val a = x
    val b = x
    return a
}

fun <!VIPER_TEXT!>pass_nullable_parameter<!>(x: Int?): Int? {
    use_nullable_twice(x)
    return x
}

fun <!VIPER_TEXT!>nullable_nullable_comparison<!>(x: Int?, y: Int?): Boolean {
    return x == y
}

fun <!VIPER_TEXT!>nullable_non_nullable_comparison<!>(x: Int?, y: Int?): Boolean {
    return x != 3
}

fun <!VIPER_TEXT!>null_comparison<!>(x: Int?): Boolean {
    return x == null
}

fun <!VIPER_TEXT!>elvis_operator<!>(x: Int?): Int {
    return x ?: 3
}

fun <!VIPER_TEXT!>elvis_operator_complex<!>(x: Int?): Int {
    return pass_nullable_parameter(x) ?: elvis_operator(2)
}

fun <!VIPER_TEXT!>elvis_operator_return<!>(x: Int?): Int {
    val y = x ?: return 0
    return y
}

fun <!VIPER_TEXT!>safe_call<!>(s: String?) {
    s?.hashCode()
}

fun <!VIPER_TEXT!>safe_call_property<!>(s: String?) {
    val l = s?.length
}

class Foo(val v: Int) {
    @NeverConvert
    fun nullable(): Foo? = this
}

fun <!VIPER_TEXT!>safe_call_chain<!>(foo: Foo?): Int? {
    return foo?.nullable()?.nullable()?.v
}
