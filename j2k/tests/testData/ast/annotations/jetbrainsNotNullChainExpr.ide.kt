package test
class Foo() {
open fun execute() {
}
}
class Bar() {
var fooNotNull : Foo = Foo()
var fooNullable : Foo = null
}
class Test() {
public open fun test(barNotNull : Bar, barNullable : Bar) {
barNotNull.fooNotNull.execute()
barNotNull.fooNullable.execute()
barNullable.fooNotNull.execute()
barNullable.fooNullable.execute()
}
}