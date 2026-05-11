package test

enum class TestEnum {
    A,
    B,
    C
}

inline fun foo(): List<Enum<*>> = TestEnum.entries.toList()
