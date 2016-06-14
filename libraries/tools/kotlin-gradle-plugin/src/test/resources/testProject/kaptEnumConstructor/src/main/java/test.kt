// See KT-12694 Java class cannot access (Kotlin) enum with String constructor param

package example

enum class TestEnum(val value: String) {
    ENTRY("");
}
