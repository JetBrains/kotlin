annotation class Hello
val v = 1

fun foo(@Volatile @<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: Hello
// EXIST: Suppress
// ABSENT: String
// ABSENT: v
