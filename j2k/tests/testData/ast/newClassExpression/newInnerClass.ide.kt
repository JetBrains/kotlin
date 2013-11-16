package org.test
open class OuterClass() {
open class InnerClass() {
}
}
open class User() {
open fun main() : Unit {
val outerObject = OuterClass()
val innerObject = outerObject.InnerClass()
}
}