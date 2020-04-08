annotation class Foo(val a: Array<String>)

@Foo(<caret>) fun foo() {}

// EXIST: "[]"
// LANGUAGE_VERSION: 1.2