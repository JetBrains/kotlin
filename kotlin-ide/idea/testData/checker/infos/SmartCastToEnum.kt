// FIR_COMPARISON

package test2

<info descr="null">enum</info> class En { A, B, С }

fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {
    val en2: Any? = En.A
    if (en2 is En) {
        when (<info descr="Smart cast to test2.En">en2</info>) {
            En.A -> {}
            En.B -> {}
            En.С -> {}
        }
    }
}
