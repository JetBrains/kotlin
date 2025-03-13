import Generics
import Testing

@Test
func smoke() throws {
    let foo = Foo()
    try #require(foo == id(param: foo))
    try #require(nil == id(param: nil))
}