package org.test
open class OuterClass() {
open class InnerClass() {
}
}
open class User() {
open fun main() {
val outerObject = OuterClass()
val innerObject = outerObject.InnerClass()
}
}