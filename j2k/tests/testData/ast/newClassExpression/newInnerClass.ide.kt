package org.test
class OuterClass() {
class InnerClass() {
}
}
class User() {
open fun main() {
val outerObject = OuterClass()
val innerObject = outerObject.InnerClass()
}
}