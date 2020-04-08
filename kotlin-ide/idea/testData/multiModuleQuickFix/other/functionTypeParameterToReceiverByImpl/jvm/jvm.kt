actual fun foo(n: Int, action: (Int) -> Int) = action(n)

fun test1() {
    foo(1) { n -> n + 1 }
}