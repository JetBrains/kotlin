import org.jetbrains.kotlin.formver.plugin.NeverConvert
import java.lang.IllegalArgumentException

@NeverConvert
fun call(x: Int) {
}

fun <!VIPER_TEXT!>try_catch<!>() {
    try {
        call(0)
        call(1)
    } catch (e: Exception) {
        call(2)
    }
}

fun <!VIPER_TEXT!>nested_try_catch<!>() {
    try {
        call(0)
        try {
            call(1)
        }
        catch (e: IllegalArgumentException) {
            call(2)
        }
    }
    catch (e: Exception) {}
}

@NeverConvert
@Suppress("NOTHING_TO_INLINE")
inline fun call_twice() {
    call(0)
    call(1)
}

fun <!VIPER_TEXT!>try_catch_with_inline<!>() {
    try {
        call_twice()
    } catch (e: Exception) {
        call(2)
    }
}

fun <!VIPER_TEXT!>try_catch_shadowing<!>() {
    val x = 0
    try {
        val x = 1
    }
    catch (e: Exception) {
        val x = 2
    }
}

fun <!VIPER_TEXT!>multiple_catches<!>() {
    try {
        call(0)
        call(1)
    }
    catch (e: IllegalArgumentException) {
        call(2)
    }
    catch (e: Exception) {
        call(3)
    }
}

@NeverConvert
fun ignore(e: Exception) {}

fun <!VIPER_TEXT!>use_exception<!>() {
    try {
        call(0)
    }
    catch (e: Exception) {
        ignore(e)
    }
}