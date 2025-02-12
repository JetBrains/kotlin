import TypeParameters
import KotlinRuntime
import Testing

@Test
func testNewClass() throws {
    let paramValue = "42"
    let foo = Foo(genericTypeParam: paramValue)
    try #require(paramValue == foo.getGenericTypeParam() as! String)
}