// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    @Suppress("Foo") ""<caret>!!
}

annotation class ann