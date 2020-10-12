const val HELLO_WORLD = "Hello, World!"
fun sayAndGetHelloWorld(): List<String> = listOf(HELLO_WORLD).als<caret>o { println(it) }