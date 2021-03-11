// "Import" "true"
package p

class T {
    companion object {
        val T.foobar get = 10
    }
}

fun usage(t: T) {
    t.<caret>foobar
}