// This test mostly checks frontend behaviour.
// TARGET_BACKEND: NATIVE
// MODULE: cinterop1
// FILE: cForwardDeclarationsTwoLibs1.def
---
struct StructDeclaredUndeclared;
struct StructDeclaredDeclared;
struct StructDeclaredDefined;
struct StructDefinedUndeclared {};
struct StructDefinedDeclared {};
struct StructDefinedDefined {};

int use1StructDeclaredUndeclared(struct StructDeclaredUndeclared* declaredUndeclared) { return -3; }
int use1StructDeclaredDeclared(struct StructDeclaredDeclared* declaredDeclared) { return -4; }
int use1StructDeclaredDefined(struct StructDeclaredDefined* declaredDefined) { return -5; }
int use1StructDefinedUndeclared(struct StructDefinedUndeclared* definedUndeclared) { return -6; }
int use1StructDefinedDeclared(struct StructDefinedDeclared* definedDeclared) { return -7; }
int use1StructDefinedDefined(struct StructDefinedDefined* definedDefined) { return -8; }

// MODULE: cinterop2
// FILE: cForwardDeclarationsTwoLibs2.def
---
struct StructUndeclaredDeclared;
struct StructUndeclaredDefined {};
struct StructDeclaredDeclared;
struct StructDeclaredDefined {};
struct StructDefinedDeclared;
struct StructDefinedDefined {};

int use2StructUndeclaredDeclared(struct StructUndeclaredDeclared* undeclaredDeclared) { return 1; }
int use2StructUndeclaredDefined(struct StructUndeclaredDefined* undeclaredDefined) { return 2; }
int use2StructDeclaredDeclared(struct StructDeclaredDeclared* declaredDeclared) { return 4; }
int use2StructDeclaredDefined(struct StructDeclaredDefined* declaredDefined) { return 5; }
int use2StructDefinedDeclared(struct StructDefinedDeclared* definedDeclared) { return 7; }
int use2StructDefinedDefined(struct StructDefinedDefined* definedDefined) { return 8; }

// MODULE: main(cinterop1, cinterop2)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cForwardDeclarationsTwoLibs1.*
import cForwardDeclarationsTwoLibs2.*
import cnames.structs.StructUndeclaredDeclared
import cnames.structs.StructDeclaredUndeclared
import cnames.structs.StructDeclaredDeclared

import kotlin.test.assertEquals
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ptr
// The test should also check that these references can't be resolved, but the test infra doesn't support this yet:
// import cForwardDeclarationsTwoLibs1.StructUndeclaredUndeclared
// import cForwardDeclarationsTwoLibs1.StructUndeclaredDeclared
// import cForwardDeclarationsTwoLibs1.StructUndeclaredDefined
// import cForwardDeclarationsTwoLibs2.StructUndeclaredUndeclared
// import cForwardDeclarationsTwoLibs2.StructDeclaredUndeclared
// import cForwardDeclarationsTwoLibs2.StructDefinedUndeclared
// import cnames.structs.StructUndeclaredUndeclared // Supported in K1 though.

fun <T1 : T2, T2> checkSubtype2() {}

// Here we rely on frontend reporting conflicting overloads if some of these types turn out to be the same.
fun checkDifferentTypes(s: StructUndeclaredDeclared?) = 1
fun checkDifferentTypes(s: StructUndeclaredDefined?) = 2
fun checkDifferentTypes(s: StructDeclaredUndeclared?) = 3
fun checkDifferentTypes(s: StructDeclaredDeclared?) = 4
fun checkDifferentTypes(s: StructDeclaredDefined?) = 5
fun checkDifferentTypes(s: StructDefinedUndeclared?) = 6
fun checkDifferentTypes(s: StructDefinedDeclared?) = 7
fun checkDifferentTypes(s: cForwardDeclarationsTwoLibs1.StructDefinedDefined?) = 8
fun checkDifferentTypes(s: cForwardDeclarationsTwoLibs2.StructDefinedDefined?) = 9

fun box(): String {
    checkSubtype2<StructUndeclaredDeclared, COpaque>()

    checkSubtype2<StructUndeclaredDefined, CStructVar>()

    checkSubtype2<StructDeclaredUndeclared, COpaque>()

    checkSubtype2<StructDeclaredDeclared, COpaque>()

    checkSubtype2<cnames.structs.StructDeclaredDefined, CPointed>()
    checkSubtype2<StructDeclaredDefined, CStructVar>()

    checkSubtype2<StructDefinedUndeclared, CStructVar>()

    checkSubtype2<cnames.structs.StructDefinedDeclared, CPointed>()
    checkSubtype2<StructDefinedDeclared, CStructVar>()

    checkSubtype2<cForwardDeclarationsTwoLibs1.StructDefinedDefined, CStructVar>()
    checkSubtype2<cForwardDeclarationsTwoLibs2.StructDefinedDefined, CStructVar>()

    val undeclaredDeclared: StructUndeclaredDeclared? = null
    val undeclaredDefined: StructUndeclaredDefined? = null
    val declaredUndeclared: StructDeclaredUndeclared? = null
    val declaredDeclared: StructDeclaredDeclared? = null
    val cnamesDeclaredDefined: cnames.structs.StructDeclaredDefined? = null
    val declaredDefined: StructDeclaredDefined? = null
    val definedUndeclared: StructDefinedUndeclared? = null
    val cnamesDefinedDeclared: cnames.structs.StructDefinedDeclared? = null
    val definedDeclared: StructDefinedDeclared? = null
    val definedDefined1: cForwardDeclarationsTwoLibs1.StructDefinedDefined? = null
    val definedDefined2: cForwardDeclarationsTwoLibs2.StructDefinedDefined? = null

    assertEquals(1, checkDifferentTypes(undeclaredDeclared))
    assertEquals(2, checkDifferentTypes(undeclaredDefined))
    assertEquals(3, checkDifferentTypes(declaredUndeclared))
    assertEquals(4, checkDifferentTypes(declaredDeclared))
    assertEquals(5, checkDifferentTypes(declaredDefined))
    assertEquals(6, checkDifferentTypes(definedUndeclared))
    assertEquals(7, checkDifferentTypes(definedDeclared))
    assertEquals(8, checkDifferentTypes(definedDefined1))
    assertEquals(9, checkDifferentTypes(definedDefined2))

    assertEquals(1, use2StructUndeclaredDeclared(undeclaredDeclared?.ptr))
    assertEquals(2, use2StructUndeclaredDefined(undeclaredDefined?.ptr))
    assertEquals(-3, use1StructDeclaredUndeclared(declaredUndeclared?.ptr))
    assertEquals(-4, use1StructDeclaredDeclared(declaredDeclared?.ptr))
    assertEquals(4, use2StructDeclaredDeclared(declaredDeclared?.ptr))
    assertEquals(-5, use1StructDeclaredDefined(cnamesDeclaredDefined?.ptr))
    assertEquals(5, use2StructDeclaredDefined(declaredDefined?.ptr))
    assertEquals(-6, use1StructDefinedUndeclared(definedUndeclared?.ptr))
    assertEquals(-7, use1StructDefinedDeclared(definedDeclared?.ptr))
    assertEquals(7, use2StructDefinedDeclared(cnamesDefinedDeclared?.ptr))
    assertEquals(-8, use1StructDefinedDefined(definedDefined1?.ptr))
    assertEquals(8, use2StructDefinedDefined(definedDefined2?.ptr))

    return "OK"
}
