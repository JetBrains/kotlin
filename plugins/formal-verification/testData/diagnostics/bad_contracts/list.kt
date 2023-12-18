// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_expr_get<!>() {
    val s = <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyList<Int>()[0]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_get<!>() {
    val myList: List<Int> = emptyList()
    val s = <!POSSIBLE_INDEX_OUT_OF_BOUND!>myList[0]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>unsafe_last<!>(l: List<Int>) : Int {
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>l[l.size - 1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>add_get<!>(l: MutableList<Int>) {
    l.add(1)
    val n = <!POSSIBLE_INDEX_OUT_OF_BOUND!>l[1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_sub<!>() {
    val l = <!INVALID_SUBLIST_RANGE!>emptyList<Int>().subList(0, 1)<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_sub_negative<!>() {
    val l = <!INVALID_SUBLIST_RANGE!>emptyList<Int>().subList(-1, 1)<!>
}