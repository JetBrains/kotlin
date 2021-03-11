// FLOW: OUT

fun f1(param: String){
    val a = "param = $param"
}

fun main(args: Array<String>) {
    val <caret>hello = "Hello"
    val v = "hello = $hello"
    f1(hello)
}