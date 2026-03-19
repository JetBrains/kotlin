import ValueClass
import KotlinRuntime
import Testing

@Test
func inlineClassWithRef() throws {
    let foo = Foo(x: 1)
    let bar = Bar(foo: foo)
    try #require(bar.foo.x == 1)
}
