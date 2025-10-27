// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: unsupportedDeclarationsReporterKind=inline
// FILE: main.kt
class Foo {
    inner class Inner {}

    class Nested {
        operator fun plus(other: Int): Nested = this
    }
}

inline fun foo() {}

// FILE: packaged.kt
package a.b.c

enum class E {
    A, B, C
}

// KT-79227 Swift Export: Fix First Release Issues
// Trampoline declarations conflict at module scope
open class A {
    fun make(value: Int): String = "A:$value"
}

open class B {
    fun make(value: Int): String = "B:$value"
}
