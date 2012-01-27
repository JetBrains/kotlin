package org.test
open class OuterClass() {
open class InnerClass() {
}
}
open class User() {
open fun main() : Unit {
var outerObject : OuterClass? = OuterClass()
var innerObject : OuterClass.InnerClass? = outerObject?.InnerClass()
}
}