annotation class Foo(val a: Array<String> = <caret>)

// EXIST: "[]"
// LANGUAGE_VERSION: 1.2