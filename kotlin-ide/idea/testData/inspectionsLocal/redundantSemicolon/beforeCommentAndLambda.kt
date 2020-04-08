// PROBLEM: none
fun test() {
    foo()<caret>;
    // comment
    { i: Int ->  }.doIt()
}
fun foo() {}
fun ((Int) -> Unit).doIt() {}
