// LANGUAGE: +RichErrors

@Target(AnnotationTarget.TYPE) annotation class Ann1
@Target(AnnotationTarget.TYPE) annotation class Ann2
@Target(AnnotationTarget.TYPE) annotation class Ann3

error class Error1
error class Error2

fun foo(x: String | Error1 | Error2) {}
fun bar(x: Error1 | Error2) {}
fun baz(): String | Error1 | Error2 = TODO()
fun foobar(): Error1 | Error2 = TODO()
fun (Int | Error1).extension() {}

val annotatedType: @Ann1 (@Ann2 String | @Ann3 Error1) = TODO()
