package test
open class Foo() {
open fun execute() {
}
}
open class Bar() {
var fooNotNull : Foo = Foo()
var fooNullable : Foo = null
}
open class Test() {
public open fun test(barNotNull : Bar, barNullable : Bar) {
barNotNull.fooNotNull.execute()
barNotNull.fooNullable.execute()
barNullable.fooNotNull.execute()
barNullable.fooNullable.execute()
}
}