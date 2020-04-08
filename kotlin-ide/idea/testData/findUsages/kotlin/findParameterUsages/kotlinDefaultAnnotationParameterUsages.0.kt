// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
annotation class KAnn(val <caret>value: String)

@KAnn("abc")
fun test1() {}

@KAnn(value = "abc")
fun test2() {}