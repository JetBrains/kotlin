// "Create member property 'A.foo'" "true"
// ERROR: Property must be initialized or be abstract

object A

fun test() {
    val a: Int = A.<caret>foo
}
