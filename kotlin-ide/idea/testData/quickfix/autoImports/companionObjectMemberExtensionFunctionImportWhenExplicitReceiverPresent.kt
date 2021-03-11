// "Import" "true"
package p

class T {
    companion object {
        fun T.foobar() {}
    }
}

fun usage(t: T) {
    t.<caret>foobar()
}