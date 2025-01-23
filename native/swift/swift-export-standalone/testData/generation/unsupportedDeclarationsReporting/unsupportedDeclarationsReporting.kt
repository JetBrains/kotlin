// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: unsupportedDeclarationsReporterKind=inline
// FILE: main.kt
class Foo {
    inner class Inner {}

    class Nested {
        operator fun plus(other: Int): Nested = this
    }

    val Unit.extPropMember
        get() = Unit
}

interface MyInterface

val Foo.extProp
    get() = Unit

inline fun foo() {}

// FILE: packaged.kt
package a.b.c

enum class E {
    A, B, C
}
