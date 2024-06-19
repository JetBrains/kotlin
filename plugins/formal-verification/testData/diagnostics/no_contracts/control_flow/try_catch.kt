// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import java.lang.IllegalArgumentException

@NeverConvert
fun call(x: Int) {
}

fun <!VIPER_TEXT!>tryCatch<!>() {
    try {
        call(0)
        call(1)
    } catch (e: Exception) {
        call(2)
    }
}

fun <!VIPER_TEXT!>nestedTryCatch<!>() {
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
inline fun callTwice() {
    call(0)
    call(1)
}

fun <!VIPER_TEXT!>tryCatchWithInline<!>() {
    try {
        callTwice()
    } catch (e: Exception) {
        call(2)
    }
}

fun <!VIPER_TEXT!>tryCatchShadowing<!>() {
    val x = 0
    try {
        val x = 1
    }
    catch (e: Exception) {
        val x = 2
    }
}

fun <!VIPER_TEXT!>multipleCatches<!>() {
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

fun <!VIPER_TEXT!>useException<!>() {
    try {
        call(0)
    }
    catch (e: Exception) {
        ignore(e)
    }
}