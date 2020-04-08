// "Change all usages of 'java.lang.Comparable<T>' in this file to 'kotlin.Comparable<T>'" "true"
import java.lang.*
import java.lang.Comparable
import java.lang.Comparable
import java.lang.Comparable as Foo

fun <T> a() : java.lang.Comparable<T><caret>? {
    return null
}

fun b() : java.lang.Comparable<String> {
    throw Exception()
}

fun c() : Foo<String> {
    throw Exception()
}

fun d() : java.lang.Comparable<String>? {
    return null
}

fun e() : Comparable<String>? {
    throw Exception()
}
