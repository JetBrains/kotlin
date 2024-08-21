// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: unsupportedDeclarationsReporterKind=inline, multipleModulesHandlingStrategy=IntoSingleModule
// FILE: main.kt
class Foo {
    inner class Inner {}

    class Nested {
        operator fun plus(other: Int): Nested = this
    }

    val Unit.extPropMember
        get() = Unit

    fun Unit.extFunMember() {}
}

interface MyInterface

fun Foo.ext() {}

val Foo.extProp
    get() = Unit

inline fun foo() {}

// FILE: packaged.kt
package a.b.c

abstract class A

enum class E {
    A, B, C
}
