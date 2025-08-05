package java.lang.annotation

typealias Annotation = kotlin.Annotation

fun Annotation.annotationType(): dynamic = js("({})")