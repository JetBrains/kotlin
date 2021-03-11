fun main(args: Array<String>) {
    val foo: String? = "foo"
    val y = if (true) foo?.<caret>length else null
}
