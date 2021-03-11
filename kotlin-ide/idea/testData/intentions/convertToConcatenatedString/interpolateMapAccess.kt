// WITH_RUNTIME
fun main(args: Array<String>){
    val x = mapOf("a" to "b")
    val y = "<caret>abcd${x["a"]}"
}
