// LANGUAGE: +RichErrors

error class Error1
error class Error2

fun foo(x: String | Error1 | Error2) {}
fun bar(x: Error1 | Error2) {}
fun baz(): String | Error1 | Error2 = TODO()
fun foobar(): Error1 | Error2 = TODO()
fun (Int | Error1).extension() {}
