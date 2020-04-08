package aaaa

fun <T> foo(s: String, t: T) {}

fun test(s: String) {
    foo<Int, String>(<caret>)
}

// EXIST: s