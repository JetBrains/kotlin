annotation class Bar(vararg val a: String = <caret>)

// EXIST: "[]"
// LANGUAGE_VERSION: 1.2