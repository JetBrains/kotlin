// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT!>empty_list_get<!>() {
    val myList: List<Int> = emptyList()
    val s = <!VIPER_VERIFICATION_ERROR!>myList[0]<!>
}

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT!>unsafe_last<!>(l: List<Int>) : Int {
    return <!VIPER_VERIFICATION_ERROR!>l[l.size - 1]<!>
}

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT!>add_get<!>(l: MutableList<Int>) {
    l.add(1)
    val n = <!VIPER_VERIFICATION_ERROR!>l[1]<!>
}
