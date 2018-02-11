class FunctionsTest {
    fun f() = String::length
    fun f2() = fun (a: Int, b: Int) = a > b
    fun f3() = run {}
    fun f4() = run { 3 }
}