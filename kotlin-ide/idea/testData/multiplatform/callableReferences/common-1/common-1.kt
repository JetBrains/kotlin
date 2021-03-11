@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface <!LINE_MARKER("descr='Is subclassed by B [common-1] B [jvm]'"), LINE_MARKER("descr='Has actuals in common'")!>A<!> {
    fun <!LINE_MARKER("descr='Has actuals in common'")!>common_1_A<!>()
}

expect interface B : A {
    fun common_1_B()
}

typealias A_Common_1_Alias = A
typealias B_Common_1_Alias = B

fun take_A_common_1(func: (A) -> Unit) {}
fun take_B_common_1(func: (B) -> Unit) {}

fun take_A_alias_common_1(func: (A_Common_1_Alias) -> Unit) {}
fun take_B_alias_common_1(func: (B_Common_1_Alias) -> Unit) {}