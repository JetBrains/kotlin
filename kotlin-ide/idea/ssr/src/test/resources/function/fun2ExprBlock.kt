fun fooEmpty() {}
fun fooSingleExpr() = 1
fun fooOneStatementInBlock() {
    println()
}
<warning descr="SSR">fun fooTwoStatementsInBlock() {
    println()
    println()
}</warning>