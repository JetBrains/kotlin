// "Remove redundant initializer" "true"
// WITH_RUNTIME
fun foo() {
    var bar = 1<caret>
    bar = 42
    println(bar)
}