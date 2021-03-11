// "Replace with 'bar(y)'" "true"

@Deprecated("", replaceWith = ReplaceWith("bar(y)"))
fun foo(x: Any, y: Any, z: Any) {
}
fun bar(y: Any) {}
fun main() {
    <caret>foo(4::class, 42::dec, ::bar)
}