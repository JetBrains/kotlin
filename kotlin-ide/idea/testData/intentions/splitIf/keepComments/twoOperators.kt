fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    if (a /*a*/ && b /*b*/ &<caret>& c /*c*/) {
        doSomething("test")
    }
}
