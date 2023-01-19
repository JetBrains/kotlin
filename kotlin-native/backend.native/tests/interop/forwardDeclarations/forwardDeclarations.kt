// This test mostly checks frontend behaviour.

import cForwardDeclarations.*

// Note: there are no practical cases for this type, but it works in K1 so better to have a test for this.
import cnames.structs.StructUndeclared

import kotlin.test.assertEquals
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ptr
// The test should also check that these references can't be resolved, but the test infra doesn't support this yet:
// import cForwardDeclarations.StructUndeclared

fun <T1 : T2, T2> checkSubtype2() {}
fun <T1 : T2, T2 : T3, T3> checkSubtype3() {}
fun <T1 : T2, T2 : T3, T3 : T4, T4> checkSubtype4() {}

// Here we rely on frontend reporting conflicting overloads if some of these types turn out to be the same.
fun checkDifferentTypes(s: StructUndeclared?) = 0
fun checkDifferentTypes(s: StructDeclared?) = 1
fun checkDifferentTypes(s: StructDefined?) = 2

fun main() {
    // Checking it is the same type:
    checkSubtype3<StructUndeclared, cnames.structs.StructUndeclared, StructUndeclared>()
    checkSubtype2<StructUndeclared, COpaque>()

    checkSubtype4<StructDeclared, cnames.structs.StructDeclared, cForwardDeclarations.StructDeclared, StructDeclared>()
    checkSubtype2<StructDeclared, COpaque>()

    checkSubtype4<StructDefined, cnames.structs.StructDefined, cForwardDeclarations.StructDefined, StructDefined>()
    checkSubtype2<StructDefined, CStructVar>()

    val undeclared: StructUndeclared? = null
    val declared: StructDeclared? = null
    val defined: StructDefined? = null

    assertEquals(0, checkDifferentTypes(undeclared))
    assertEquals(1, checkDifferentTypes(declared))
    assertEquals(2, checkDifferentTypes(defined))

    assertEquals(-1, useStructDeclared(declared?.ptr))
    assertEquals(-2, useStructDefined(defined?.ptr))
}
