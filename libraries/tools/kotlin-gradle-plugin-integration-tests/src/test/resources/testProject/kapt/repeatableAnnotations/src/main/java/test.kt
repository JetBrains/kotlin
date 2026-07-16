package example

annotation class ToBeChecked

@Repeatable
annotation class Anno(val value: String)

@ToBeChecked
@Anno("1")
@Anno("2")
public class TestClass
