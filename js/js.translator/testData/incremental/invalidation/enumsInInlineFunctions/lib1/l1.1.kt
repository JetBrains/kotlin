class TestClass1 {
    enum class TestEnum {
        A, B
    }
}

class TestClass2 {
    enum class TestEnum {
        A, B, C
    }
}

inline fun foo1(): List<Enum<*>> {
    return TestClass1.TestEnum.values().toList()
}

inline fun foo2(): List<Enum<*>> {
    return TestClass2.TestEnum.values().toList()
}

inline fun foo3(): List<Enum<*>> {
    return emptyList()
}
