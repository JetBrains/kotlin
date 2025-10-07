// This test mostly checks frontend behaviour.

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cForwardDeclarations.def
---
struct StructDeclared;
struct StructDefined { int x; };

int useStructDeclared(struct StructDeclared* declared) {
    return -1;
}

int useStructDefined(struct StructDefined* defined) {
    return -2;
}


// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cForwardDeclarations.*
import cnames.structs.StructDeclared

import kotlin.test.assertEquals
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ptr
// The test should also check that these references can't be resolved, but the test infra doesn't support this yet:
// import cForwardDeclarations.StructUndeclared
// import cnames.structs.StructUndeclared // Supported in K1 though.

fun <T1 : T2, T2> checkSubtype2() {}

// Here we rely on frontend reporting conflicting overloads if some of these types turn out to be the same.
fun checkDifferentTypes(s: StructDeclared?) = 1
fun checkDifferentTypes(s: StructDefined?) = 2

fun box(): String {
    checkSubtype2<StructDeclared, COpaque>()

    checkSubtype2<StructDefined, CStructVar>()

    val declared: StructDeclared? = null
    val defined: StructDefined? = null

    assertEquals(1, checkDifferentTypes(declared))
    assertEquals(2, checkDifferentTypes(defined))

    assertEquals(-1, useStructDeclared(declared?.ptr))
    assertEquals(-2, useStructDefined(defined?.ptr))

    return "OK"
}
