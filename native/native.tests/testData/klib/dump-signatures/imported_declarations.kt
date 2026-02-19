// FIR_IDENTICAL

package imported

private fun consume(anything: Any?) {
    anything.toString()
}

fun test() {
    consume(foo.publicFun())
    consume(bar.publicVal)
    consume(foo.PublicClass())
}
