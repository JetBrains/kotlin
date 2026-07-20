package example

@example.ExampleAnnotation
public class TestClass {

    @example.ExampleAnnotation
    public val testVal: String = "text"

    @example.ExampleAnnotation
    public fun testFunction(): Class<*> = TestClassCustomized::class.java

}