fun foo(arg: String?): Int? = arg?.<caret>length
fun main(args: Array<String>) {
    foo("bar")
}
