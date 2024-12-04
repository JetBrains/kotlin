class TestClass2 {
    enum class TestEnum {
        A, B
    }
}

fun testEnums(): List<Enum<*>> {
    val enums1 = foo1()
    val enums3 = foo3()

    return TestClass2.TestEnum.values().toList() + enums1 + enums3
}
