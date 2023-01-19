// This test mostly checks frontend behaviour.

import cForwardDeclarationsTwoLibs1.*
import cForwardDeclarationsTwoLibs2.*

// Note: there are no practical cases for this type, but it works in K1 so better to have a test for this.
import cnames.structs.StructUndeclaredUndeclared

import kotlin.test.assertEquals
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ptr
// The test should also check that these references can't be resolved, but the test infra doesn't support this yet:
// import cForwardDeclarationsTwoLibs1.StructUndeclaredUndeclared
// import cForwardDeclarationsTwoLibs1.StructUndeclaredDeclared
// import cForwardDeclarationsTwoLibs1.StructUndeclaredDefined
// import cForwardDeclarationsTwoLibs2.StructUndeclaredUndeclared
// import cForwardDeclarationsTwoLibs2.StructDeclaredUndeclared
// import cForwardDeclarationsTwoLibs2.StructDefinedUndeclared

fun <T1 : T2, T2> checkSubtype2() {}
fun <T1 : T2, T2 : T3, T3> checkSubtype3() {}
fun <T1 : T2, T2 : T3, T3 : T4, T4> checkSubtype4() {}
fun <T1 : T2, T2 : T3, T3 : T4, T4 : T5, T5> checkSubtype5() {}
fun <T1 : T2, T2 : T3, T3 : T4, T4 : T5, T5 : T6, T6> checkSubtype6() {}

// Here we rely on frontend reporting conflicting overloads if some of these types turn out to be the same.
fun checkDifferentTypes(s: StructUndeclaredUndeclared?) = 0
fun checkDifferentTypes(s: StructUndeclaredDeclared?) = 1
fun checkDifferentTypes(s: StructUndeclaredDefined?) = 2
fun checkDifferentTypes(s: StructDeclaredUndeclared?) = 3
fun checkDifferentTypes(s: StructDeclaredDeclared?) = 4
fun checkDifferentTypes(s: StructDeclaredDefined?) = 5
fun checkDifferentTypes(s: StructDefinedUndeclared?) = 6
fun checkDifferentTypes(s: StructDefinedDeclared?) = 7
fun checkDifferentTypes(s: cForwardDeclarationsTwoLibs1.StructDefinedDefined?) = 8
fun checkDifferentTypes(s: cForwardDeclarationsTwoLibs2.StructDefinedDefined?) = 9

fun main() {
    // Checking it is the same type:
    checkSubtype3<StructUndeclaredUndeclared, cnames.structs.StructUndeclaredUndeclared, StructUndeclaredUndeclared>()
    checkSubtype2<StructUndeclaredUndeclared, COpaque>()

    checkSubtype4<
            StructUndeclaredDeclared,
            cnames.structs.StructUndeclaredDeclared,
            cForwardDeclarationsTwoLibs2.StructUndeclaredDeclared,
            StructUndeclaredDeclared,
    >()
    checkSubtype2<StructUndeclaredDeclared, COpaque>()

    checkSubtype4<
            StructUndeclaredDefined,
            cnames.structs.StructUndeclaredDefined,
            cForwardDeclarationsTwoLibs2.StructUndeclaredDefined,
            StructUndeclaredDefined
    >()
    checkSubtype2<StructUndeclaredDefined, CStructVar>()

    checkSubtype4<
            StructDeclaredUndeclared,
            cnames.structs.StructDeclaredUndeclared,
            cForwardDeclarationsTwoLibs1.StructDeclaredUndeclared,
            StructDeclaredUndeclared
    >()
    checkSubtype2<StructDeclaredUndeclared, COpaque>()

    checkSubtype5<
            StructDeclaredDeclared,
            cnames.structs.StructDeclaredDeclared,
            cForwardDeclarationsTwoLibs1.StructDeclaredDeclared,
            cForwardDeclarationsTwoLibs2.StructDeclaredDeclared,
            StructDeclaredDeclared
    >()
    checkSubtype2<StructDeclaredDeclared, COpaque>()

    checkSubtype5<
            StructDeclaredDefined,
            cnames.structs.StructDeclaredDefined,
            cForwardDeclarationsTwoLibs1.StructDeclaredDefined,
            cForwardDeclarationsTwoLibs2.StructDeclaredDefined,
            StructDeclaredDefined
    >()
    checkSubtype2<StructDeclaredDefined, CStructVar>()

    checkSubtype4<
            StructDefinedUndeclared,
            cnames.structs.StructDefinedUndeclared,
            cForwardDeclarationsTwoLibs1.StructDefinedUndeclared,
            StructDefinedUndeclared
    >()
    checkSubtype2<StructDefinedUndeclared, CStructVar>()

    checkSubtype5<
            StructDefinedDeclared,
            cnames.structs.StructDefinedDeclared,
            cForwardDeclarationsTwoLibs1.StructDefinedDeclared,
            cForwardDeclarationsTwoLibs2.StructDefinedDeclared,
            StructDefinedDeclared
    >()
    checkSubtype2<StructDefinedDeclared, CStructVar>()

    checkSubtype2<cnames.structs.StructDefinedDefined, CStructVar>()
    checkSubtype2<cForwardDeclarationsTwoLibs1.StructDefinedDefined, CStructVar>()
    checkSubtype2<cForwardDeclarationsTwoLibs2.StructDefinedDefined, CStructVar>()

    val undeclaredUndeclared: StructUndeclaredUndeclared? = null
    val undeclaredDeclared: StructUndeclaredDeclared? = null
    val undeclaredDefined: StructUndeclaredDefined? = null
    val declaredUndeclared: StructDeclaredUndeclared? = null
    val declaredDeclared: StructDeclaredDeclared? = null
    val declaredDefined: StructDeclaredDefined? = null
    val definedUndeclared: StructDefinedUndeclared? = null
    val definedDeclared: StructDefinedDeclared? = null
    val definedDefined1: cForwardDeclarationsTwoLibs1.StructDefinedDefined? = null
    val definedDefined2: cForwardDeclarationsTwoLibs2.StructDefinedDefined? = null

    assertEquals(0, checkDifferentTypes(undeclaredUndeclared))
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
    assertEquals(-5, use1StructDeclaredDefined(declaredDefined?.ptr))
    assertEquals(5, use2StructDeclaredDefined(declaredDefined?.ptr))
    assertEquals(-6, use1StructDefinedUndeclared(definedUndeclared?.ptr))
    assertEquals(-7, use1StructDefinedDeclared(definedDeclared?.ptr))
    assertEquals(7, use2StructDefinedDeclared(definedDeclared?.ptr))
    assertEquals(-8, use1StructDefinedDefined(definedDefined1?.ptr))
    assertEquals(8, use2StructDefinedDefined(definedDefined2?.ptr))
}
