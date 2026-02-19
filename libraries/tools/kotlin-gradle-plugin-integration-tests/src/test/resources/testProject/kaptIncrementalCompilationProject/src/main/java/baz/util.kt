package baz

@field:example.ExampleAnnotation
val valUtil = 0

@example.ExampleAnnotation
fun funUtil() {}

fun notAnnotatedFun() {}

fun functionWithBody() {
    if (2 * 2 == 4) {
        // All's right
    }
}

@example.ExampleAnnotation
fun funGetsInputParams() {
    val input = "This is a non-test string"
    val output = ArrayList<String>()
    input.split("\\s+".toRegex())
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { word ->
            output.add(word)
        }
}