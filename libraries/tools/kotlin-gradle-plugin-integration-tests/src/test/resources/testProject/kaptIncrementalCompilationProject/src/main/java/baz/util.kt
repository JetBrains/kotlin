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