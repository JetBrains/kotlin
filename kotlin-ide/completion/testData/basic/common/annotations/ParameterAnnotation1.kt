annotation class Hello
val v = 1

fun foo(@<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: Hello
// EXIST: Suppress
// ABSENT: String
// ABSENT: v
