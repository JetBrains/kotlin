package example

annotation class Generate

@Generate
class TestClass

fun testClass(): TestClassGenerated = TestClassGenerated()
