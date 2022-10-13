class TestClass100 {
    enum class TestEnum {
        A, B
    }
}

fun testEnums(): List<Enum<*>> {
    val enums1 = foo1()
    val enums2 = foo2()

    return TestClass100.TestEnum.values().toList() + enums1 + enums2
}
