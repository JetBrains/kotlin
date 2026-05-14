package test

enum class TestEnum {
    B,
    A,
    C,
    D
}

inline fun foo(): List<Enum<*>> = TestEnum.entries.toList()
