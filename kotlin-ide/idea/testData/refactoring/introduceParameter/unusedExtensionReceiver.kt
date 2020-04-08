// WITH_RUNTIME
// WITH_DEFAULT_VALUE: false
class A(val n: Int)

fun A.foo(): Int {
    return <selection>n + 1</selection>
}

fun test() {
    A(1).foo()
    with(A(1)) {
        foo()
    }
}