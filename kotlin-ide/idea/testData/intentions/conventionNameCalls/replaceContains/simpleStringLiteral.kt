@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
public operator fun CharSequence.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = false
fun test() {
    val foo = "foo"
    foo.c<caret>ontains("bar")
}
