fun doSomeWork(): String = TODO()
fun doAndPrint() = doSomeWork().als<caret>o(::println)