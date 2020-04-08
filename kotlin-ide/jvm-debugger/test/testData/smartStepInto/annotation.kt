fun foo() {
    @Ann() @Ann val a = bar()<caret>
}

annotation class Ann

fun bar() = 1

// EXISTS: bar()