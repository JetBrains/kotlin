// FIR_COMPARISON

fun foo() {
    class C {
        <error>val x</error> = 1
        <error>fun getX()</error> = 1
    }
}