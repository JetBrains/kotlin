class TestClass1 {
    enum class TestEnum {
        A, B
    }
}

class TestClass3 {
    enum class TestEnum {
        A
    }
}

inline fun foo1(): List<Enum<*>> {
    return TestClass1.TestEnum.values().toList()
}

inline fun foo2(): List<Enum<*>> {
    return emptyList()
}

inline fun foo3(): List<Enum<*>> {
    return TestClass3.TestEnum.values().toList()
}
