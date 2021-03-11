// "Import" "true"
package p

class T

object TopLevelObject1 {
    fun <A> A.foobar() {}
}

fun usage(t: T) {
    t.<caret>foobar()
}