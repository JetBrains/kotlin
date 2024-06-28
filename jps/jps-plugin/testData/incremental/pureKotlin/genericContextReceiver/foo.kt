package test

context(T)
fun <T> foo() {}

context(T)
val <T> T.foo get() = 1