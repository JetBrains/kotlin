// WITH_RUNTIME
fun main(args: Array<String>){
    val x = "abcd"
    val y = "<caret>$x${x.reversed()}"
}
