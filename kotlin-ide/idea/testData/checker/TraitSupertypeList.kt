open class bar()

interface Foo<error>()</error> : <error>bar</error><error>()</error>, <error><error>bar</error></error>, <error><error>bar</error></error> {
}

interface Foo2 : <error>bar</error>, Foo {
}

open class Foo1() : bar(), <error>bar</error>, Foo, <error>Foo</error>() {}
open class Foo12 : bar(), <error>bar</error> {}
