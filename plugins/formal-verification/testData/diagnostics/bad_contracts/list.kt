// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>empty_list_get<!>() {
    val myList: List<Int> = emptyList()
    val s = myList[0]
}

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>unsafe_last<!>(l: List<Int>) : Int {
    return l[l.size - 1]
}

@AlwaysVerify
fun <!FUNCTION_WITH_UNVERIFIED_CONTRACT, VIPER_TEXT, VIPER_VERIFICATION_ERROR!>add_get<!>(l: MutableList<Int>) {
    l.add(1)
    val n = l[1]
}
