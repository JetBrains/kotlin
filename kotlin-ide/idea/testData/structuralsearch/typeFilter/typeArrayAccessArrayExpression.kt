val x = Array( 1, { 1 })
val y = "hello"

fun foo() {
    print(<warning descr="SSR">x[0]</warning>)
    print(y[0])
}