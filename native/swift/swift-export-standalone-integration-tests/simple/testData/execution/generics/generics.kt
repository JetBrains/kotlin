// KIND: STANDALONE
// MODULE: Generics
// FILE: generics.kt
class Foo

fun <T> id(param: T): T {
    return param
}