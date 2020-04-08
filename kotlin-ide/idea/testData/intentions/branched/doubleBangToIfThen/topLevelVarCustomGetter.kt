// WITH_RUNTIME
var a: String?
    get() = ""
    set(v) {}

fun main(args: Array<String>) {
    doSomething(a<caret>!!)
}

fun doSomething(a: Any){}
