// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.ExperimentalMultiplatform
// NO_READ_WRITE_COMPARE
// IGNORE_BACKEND_K1: JVM_IR

// MODULE: common
// FILE: common.kt

package test

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val a: Array<String>)

@OptionalExpectation
expect annotation class C()

@OptionalExpectation
expect annotation class D()

@A(42)
@B(["OK", ""])
@C
@D()
fun ok() {}

// MODULE: jvm()()(common)
// FILE: jvm.kt

package test

actual annotation class D actual constructor()
