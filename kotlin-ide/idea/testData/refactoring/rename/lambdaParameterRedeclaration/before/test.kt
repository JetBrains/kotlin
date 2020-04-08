package test

fun foo(f: (Int, Int) -> Int) = f(1, 2)

fun test() {
    foo { /*rename*/a, b -> a + b }
}