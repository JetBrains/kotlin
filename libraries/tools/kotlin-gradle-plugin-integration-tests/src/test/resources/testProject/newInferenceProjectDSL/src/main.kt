package test

fun foo(x: Int, vararg y: String) {}

fun use0(f: (Int) -> Unit) {}
fun use1(f: (Int, String) -> Unit) {}
fun use2(f: (Int, String, String) -> Unit) {}

fun testNewInference() {
    use0(::foo) // should be OK
    use1(::foo) // should be OK
    use2(::foo) // should be OK
}