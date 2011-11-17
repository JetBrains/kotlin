open class OuterClass() {
open class InnerClass() {
}
}
open class User() {
open fun main() : Unit {
var outerObject : OuterClass? = OuterClass()
var innerObject : InnerClass? = outerObject?.InnerClass()
}
}