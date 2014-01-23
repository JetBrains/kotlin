package org.test

class OuterClass() {
    class InnerClass() {
    }
}

class User() {
    fun main() {
        val outerObject = OuterClass()
        val innerObject = outerObject.InnerClass()
    }
}