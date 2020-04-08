class Sub : JavaSuper() {
    override fun bar(a: Int) {
    }

    override fun bar() {
    }
}

fun main(args: Array<String>) {
    JavaSuper().bar()
    JavaSuper().bar(1)
    Sub().bar()
    Sub().bar(1)
}