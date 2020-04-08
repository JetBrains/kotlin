// WITH_RUNTIME
fun main(args: Array<String>){
    val y = "cde"
    val z = "<caret>${listOf(1, 2)}$y.bar"
}
