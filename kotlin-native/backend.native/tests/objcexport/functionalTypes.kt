/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package functionalTypes

import kotlin.test.*

typealias AN = Any?

typealias F2 = (AN, AN) -> AN
typealias F5 = (AN, AN, AN, AN, AN) -> AN
typealias F6 = (AN, AN, AN, AN, AN, AN,) -> AN
typealias F32 = (AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN) -> AN
typealias F33 = (AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN) -> AN

fun callDynType2(list: List<F2>, param: AN) {
    val fct = list.first()
    val ret = fct(param, null)
    assertEquals(param, ret)
}

fun callStaticType2(fct: F2, param: AN) {
    val ret = fct(param, null)
    assertEquals(param, ret)
}

fun callDynType32(list: List<F32>, param: AN) {
    val fct = list.first()
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callStaticType32(fct: F32, param: AN) {
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callDynType33(list: List<F33>, param: AN) {
    val fct = list.first()
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callStaticType33(fct: F33, param: AN) {
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

abstract class FHolder {
    abstract val value: Any?
}

// Note: can't provoke dynamic function type conversion using list (as above) or generics
// due to Swift <-> Obj-C interop bugs/limitations.
// Use covariant return type instead:
class F2Holder(override val value: F2) : FHolder()

fun getDynTypeLambda2(): F2Holder = F2Holder({ p1, _ -> p1 })
fun getStaticLambda2(): F2 = { p1, _ -> p1 }

private fun f2(p1: AN, p2: AN): AN = p1

fun getDynTypeRef2(): F2Holder = F2Holder(::f2)
fun getStaticRef2(): F2 = ::f2

private fun f32(
        p1: AN, p2: AN, p3: AN, p4: AN, p5: AN, p6: AN, p7: AN, p8: AN,
        p9: AN, p10: AN, p11: AN, p12: AN, p13: AN, p14: AN, p15: AN, p16: AN,
        p17: AN, p18: AN, p19: AN, p20: AN, p21: AN, p22: AN, p23: AN, p24: AN,
        p25: AN, p26: AN, p27: AN, p28: AN, p29: AN, p30: AN, p31: AN, p32: AN
): AN = p1

private fun f33(
        p1: AN, p2: AN, p3: AN, p4: AN, p5: AN, p6: AN, p7: AN, p8: AN,
        p9: AN, p10: AN, p11: AN, p12: AN, p13: AN, p14: AN, p15: AN, p16: AN,
        p17: AN, p18: AN, p19: AN, p20: AN, p21: AN, p22: AN, p23: AN, p24: AN,
        p25: AN, p26: AN, p27: AN, p28: AN, p29: AN, p30: AN, p31: AN, p32: AN,
        p33: AN
): AN = p1

class F32Holder(override val value: F32) : FHolder()

fun getDynType32(): F32Holder = F32Holder(::f32)
fun getStaticType32(): F32 = ::f32

class F33Holder(override val value: F33) : FHolder()

fun getDynTypeRef33(): F33Holder = F33Holder(::f33)
fun getStaticTypeRef33(): F33 = ::f33

fun getDynTypeLambda33(): F33Holder = F33Holder(getStaticTypeLambda33())
fun getStaticTypeLambda33(): F33 = {
    p,
    _, _, _, _, _, _, _, _,
    _, _, _, _, _, _, _, _,
    _, _, _, _, _, _, _, _,
    _, _, _, _, _, _, _, _
    ->
    p
}
