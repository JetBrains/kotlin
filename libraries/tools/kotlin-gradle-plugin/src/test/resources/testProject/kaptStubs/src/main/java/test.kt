package example

@example.ExampleAnnotation
@example.ExampleSourceAnnotation
@example.ExampleBinaryAnnotation
@example.ExampleRuntimeAnnotation
public class TestClass {

    @example.ExampleAnnotation
    public val testVal: String = "text"

    @example.ExampleAnnotation
    public fun testFunction(): TestClassGenerated = TestClassGenerated()

}