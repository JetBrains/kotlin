// IGNORE K2
// !LANGUAGE: +MultiPlatformProjects
// !OPT_IN: kotlin.ExperimentalMultiplatform
// NO_READ_WRITE_COMPARE

package test

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val a: Array<String>)

@OptionalExpectation
expect annotation class C()

@OptionalExpectation
expect annotation class D()

actual annotation class D actual constructor()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@A(42)
@B(["OK", ""])
@C
@D()
fun ok() {}
