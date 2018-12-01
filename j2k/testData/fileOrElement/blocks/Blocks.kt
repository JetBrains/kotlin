fun bar(a: Int) {}
fun foo() {
    run {
        val a = 1
        bar(a)
    }

    run {
        val a = 2
        bar(a)
    }

    run { bar(3) }
}