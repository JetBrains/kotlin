class TestClass1 {
    enum class TestEnum {
        A, B
    }
}

inline fun foo1(): List<Enum<*>> {
    return TestClass1.TestEnum.values().toList()
}

inline fun foo2(): List<Enum<*>> {
    return emptyList()
}
