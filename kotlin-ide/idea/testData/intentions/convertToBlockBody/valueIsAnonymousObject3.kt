interface I1
interface I2

fun f() {
    fun g() = <caret>object : I1, I2 { }
}
