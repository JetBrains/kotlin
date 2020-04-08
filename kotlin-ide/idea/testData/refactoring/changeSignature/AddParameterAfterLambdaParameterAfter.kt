fun foo(block: () -> Unit, i: Int) {
}

fun test() {
    foo({}, 0)
}