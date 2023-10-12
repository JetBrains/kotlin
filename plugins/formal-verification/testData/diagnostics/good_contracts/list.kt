// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

fun <!VIPER_TEXT!>declaration<!>() {
    val l1: List<Int>
    val l2: List<Int>?
    val l3: List<List<Int>>
}

@AlwaysVerify
fun <!VIPER_TEXT!>initialization<!>(l: List<Int>) {
    val myList = l
    val myEmptyList = emptyList<Int>()
}

@AlwaysVerify
fun <!VIPER_TEXT!>add_get<!>(l: MutableList<Int>) {
    l.add(1)
    val n = l[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>last_or_null<!>(l: List<Int>) : Int? {
    val size = l.size
    if (size != 0) {
        return l[size - 1]
    } else {
        return null
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>is_empty<!>(l: List<Int>) : Int {
    return if (!l.isEmpty()) {
        l[0]
    } else {
        1
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullable_list<!>(l: List<Int>?) {
    if (l != null && !l.isEmpty()) {
        val x = l[l.size - 1]
    }
}