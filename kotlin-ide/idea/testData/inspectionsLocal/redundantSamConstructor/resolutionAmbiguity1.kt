// PROBLEM: none

fun usage(javaTest: JavaTest) {
    javaTest.foo(JavaTest.FunInterface1<caret> { 10 }) // foo call will be ambiguous without SAM constructor
}