package org.test

internal class OuterClass {
    internal inner class InnerClass
}

internal class User {
    fun main() {
        val outerObject = OuterClass()
        val innerObject = outerObject.InnerClass()
    }
}