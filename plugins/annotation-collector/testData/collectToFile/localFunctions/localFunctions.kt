package org.test

class Class {
    fun method() {
        @java.lang.Deprecated fun localFunctionInsideMethod() {}
    }
}

fun function() {
    @java.lang.Deprecated fun localFunctionInsideFunction() {}
}