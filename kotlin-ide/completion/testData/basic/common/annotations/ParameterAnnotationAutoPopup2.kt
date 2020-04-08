val v = 1

fun foo(@[volatile S<caret>) { }

// INVOCATION_COUNT: 0
// EXIST: Suppress
// ABSENT: String
// ABSENT: v
