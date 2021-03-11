// "Add annotation target" "true"
annotation class Foo

fun test() {
    var v = 0
    <caret>@Foo v++
}