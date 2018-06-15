// EXPECTED_REACHABLE_NODES: 1232
package foo


interface IA
interface IB : IA // B
interface IC : IA, IB // B, C
interface ID : IB // B
interface IE // E
interface IF : IB, IE // B, E
interface IG : ID // B
interface IH
interface II : IH // I
interface IJ : IF, II // B, E, I
interface IK : IC // B, C
interface IL: IK, IF, IJ // B, C, E, I


open class A //
open class AIA: A(), IA //
open class AIB: A(), IB // B
open class AIE: A(), IE // E
open class AIF: A(), IF // B, E
open class AIJ: A(), IJ // B, E, I
open class BAIA: AIA(), IC // B, C
open class BAIAIF: BAIA(), IF // B, C, E
open class BAIAIJ: BAIA(), IJ // B, C, E, I
open class C //
open class CIB: C(), IB // B
open class D: BAIAIF(), IG // B, C, E
open class E: A(), IF // B, E
open class F: AIB(), IK // B, C
open class G: E(), IL, IE // B, C, E, I

val an = Any()
val a = A()
val aia = AIA()
val aib = AIB()
val aie = AIE()
val aif = AIF()
val aij = AIJ()
val baia = BAIA()
val baiaif = BAIAIF()
val baiaij = BAIAIJ()
val c = C()
val cib = CIB()
val d = D()
val e = E()
val f = F()
val g = G()

var dyn = js("({})")

fun dToNB(dd: dynamic) = dd as? IB?
fun dToB(dd: dynamic) = dd as? IB

fun NBtoB(bb: IB?) = bb as? IB
fun BtoNB(bb: IB) = bb as? IB?

fun nullToNullB(aa: Any?) = aa as? IB?
fun notNullToNullB(aa: Any) = aa as? IB?
fun nullToNotNullB(aa: Any?) = aa as? IB
fun notNullToNotNullB(aa: Any) = aa as? IB

fun nullToNullC(aa: Any?) = aa as? IC?
fun notNullToNullC(aa: Any) = aa as? IC?
fun nullToNotNullC(aa: Any?) = aa as? IC
fun notNullToNotNullC(aa: Any) = aa as? IC

fun nullToNullE(aa: Any?) = aa as? IE?
fun notNullToNullE(aa: Any) = aa as? IE?
fun nullToNotNullE(aa: Any?) = aa as? IE
fun notNullToNotNullE(aa: Any) = aa as? IE

fun nullToNullI(aa: Any?) = aa as? II?
fun notNullToNullI(aa: Any) = aa as? II?
fun nullToNotNullI(aa: Any?) = aa as? II
fun notNullToNotNullI(aa: Any) = aa as? II

fun testInterfaceCastN2N(): String {
    assertEquals(null, nullToNullB(null), "null as? IB?")
    assertEquals(null, nullToNullB(a), "A? as? IB?")
    assertEquals(null, nullToNullB(aia), "AIA? as? IB?")
    assertEquals(aib, nullToNullB(aib), "AIB? as? IB?")
    assertEquals(null, nullToNullB(aie), "AIE? as? IB?")
    assertEquals(aif, nullToNullB(aif), "AIF? as? IB?")
    assertEquals(aij, nullToNullB(aij), "AIJ? as? IB?")
    assertEquals(baia, nullToNullB(baia), "BAIA? as? IB?")
    assertEquals(baiaif, nullToNullB(baiaif), "BAIAIF? as? IB?")
    assertEquals(baiaij, nullToNullB(baiaij), "BAIAIJ? as? IB?")
    assertEquals(null, nullToNullB(c), "C? as? IB?")
    assertEquals(cib, nullToNullB(cib), "CIB? as? IB?")
    assertEquals(d, nullToNullB(d), "D? as? IB?")
    assertEquals(e, nullToNullB(e), "E? as? IB?")
    assertEquals(f, nullToNullB(f), "F? as? IB?")
    assertEquals(g, nullToNullB(g), "G? as? IB?")
    assertEquals(null, nullToNullB(an), "Any? as? IB?")

    assertEquals(null, nullToNullC(null), "null as? IC?")
    assertEquals(null, nullToNullC(a), "A? as? IC?")
    assertEquals(null, nullToNullC(aia), "AIA? as? IC?")
    assertEquals(null, nullToNullC(aib), "AIB? as? IC?")
    assertEquals(null, nullToNullC(aie), "AIE? as? IC?")
    assertEquals(null, nullToNullC(aif), "AIF? as? IC?")
    assertEquals(null, nullToNullC(aij), "AIJ? as? IC?")
    assertEquals(baia, nullToNullC(baia), "BAIA? as? IC?")
    assertEquals(baiaif, nullToNullC(baiaif), "BAIAIF? as? IC?")
    assertEquals(baiaij, nullToNullC(baiaij), "BAIAIJ? as? IC?")
    assertEquals(null, nullToNullC(c), "C? as? IC?")
    assertEquals(null, nullToNullC(cib), "CIB? as? IC?")
    assertEquals(d, nullToNullC(d), "D? as? IC?")
    assertEquals(null, nullToNullC(e), "E? as? IC?")
    assertEquals(f, nullToNullC(f), "F? as? IC?")
    assertEquals(g, nullToNullC(g), "G? as? IC?")
    assertEquals(null, nullToNullC(an), "Any? as? IC?")

    assertEquals(null, nullToNullE(null), "null as? IE?")
    assertEquals(null, nullToNullE(a), "A? as? IE?")
    assertEquals(null, nullToNullE(aia), "AIA? as? IE?")
    assertEquals(null, nullToNullE(aib), "AIB? as? IE?")
    assertEquals(aie, nullToNullE(aie), "AIE? as? IE?")
    assertEquals(aif, nullToNullE(aif), "AIF? as? IE?")
    assertEquals(aij, nullToNullE(aij), "AIJ? as? IE?")
    assertEquals(null, nullToNullE(baia), "BAIA? as? IE?")
    assertEquals(baiaif, nullToNullE(baiaif), "BAIAIF? as? IE?")
    assertEquals(baiaij, nullToNullE(baiaij), "BAIAIJ? as? IE?")
    assertEquals(null, nullToNullE(c), "C? as? IE?")
    assertEquals(null, nullToNullE(cib), "CIB? as? IE?")
    assertEquals(d, nullToNullE(d), "D? as? IE?")
    assertEquals(e, nullToNullE(e), "E? as? IE?")
    assertEquals(null, nullToNullE(f), "F? as? IE?")
    assertEquals(g, nullToNullE(g), "G? as? IE?")
    assertEquals(null, nullToNullE(an), "Any? as? IE?")

    assertEquals(null, nullToNullI(null), "null as? II?")
    assertEquals(null, nullToNullI(a), "A? as? II?")
    assertEquals(null, nullToNullI(aia), "AIA? as? II?")
    assertEquals(null, nullToNullI(aib), "AIB? as? II?")
    assertEquals(null, nullToNullI(aie), "AIE? as? II?")
    assertEquals(null, nullToNullI(aif), "AIF? as? II?")
    assertEquals(aij, nullToNullI(aij), "AIJ? as? II?")
    assertEquals(null, nullToNullI(baia), "BAIA? as? II?")
    assertEquals(null, nullToNullI(baiaif), "BAIAIF? as? II?")
    assertEquals(baiaij, nullToNullI(baiaij), "BAIAIJ? as? II?")
    assertEquals(null, nullToNullI(c), "C? as? II?")
    assertEquals(null, nullToNullI(cib), "CIB? as? II?")
    assertEquals(null, nullToNullI(d), "D? as? II?")
    assertEquals(null, nullToNullI(e), "E? as? II?")
    assertEquals(null, nullToNullI(f), "F? as? II?")
    assertEquals(g, nullToNullI(g), "G? as? II?")
    assertEquals(null, nullToNullI(an), "Any? as? II?")

    assertEquals(null, dToNB(null), "null dynamic as? IB?")

    assertEquals(null, nullToNullI({}), "Function? as? II?")
    assertEquals(null, nullToNullI(true), "Boolean? as? II?")
    assertEquals(null, nullToNullI(42), "Number? as? II?")
    assertEquals(null, nullToNullI("String"), "String? as? II?")

    return "OK"
}

fun testInterfaceCastN2NN(): String {

    assertEquals(null, nullToNotNullB(null), "null as? IB")
    assertEquals(null, nullToNotNullB(a), "A? as? IB")
    assertEquals(null, nullToNotNullB(aia), "AIA? as? IB")
    assertEquals(aib, nullToNotNullB(aib), "AIB? as? IB")
    assertEquals(null, nullToNotNullB(aie), "AIE? as? IB")
    assertEquals(aif, nullToNotNullB(aif), "AIF? as? IB")
    assertEquals(aij, nullToNotNullB(aij), "AIJ? as? IB")
    assertEquals(baia, nullToNotNullB(baia), "BAIA? as? IB")
    assertEquals(baiaif, nullToNotNullB(baiaif), "BAIAIF? as? IB")
    assertEquals(baiaij, nullToNotNullB(baiaij), "BAIAIJ? as? IB")
    assertEquals(null, nullToNotNullB(c), "C? as? IB")
    assertEquals(cib, nullToNotNullB(cib), "CIB? as? IB")
    assertEquals(d, nullToNotNullB(d), "D? as? IB")
    assertEquals(e, nullToNotNullB(e), "E? as? IB")
    assertEquals(f, nullToNotNullB(f), "F? as? IB")
    assertEquals(g, nullToNotNullB(g), "G? as? IB")
    assertEquals(null, nullToNotNullB(an), "Any? as? IB")

    assertEquals(null, nullToNotNullC(null), "null as? IC")
    assertEquals(null, nullToNotNullC(a), "A? as? IC")
    assertEquals(null, nullToNotNullC(aia), "AIA? as? IC")
    assertEquals(null, nullToNotNullC(aib), "AIB? as? IC")
    assertEquals(null, nullToNotNullC(aie), "AIE? as? IC")
    assertEquals(null, nullToNotNullC(aif), "AIF? as? IC")
    assertEquals(null, nullToNotNullC(aij), "AIJ? as? IC")
    assertEquals(baia, nullToNotNullC(baia), "BAIA? as? IC")
    assertEquals(baiaif, nullToNotNullC(baiaif), "BAIAIF? as? IC")
    assertEquals(baiaij, nullToNotNullC(baiaij), "BAIAIJ? as? IC")
    assertEquals(null, nullToNotNullC(c), "C? as? IC")
    assertEquals(null, nullToNotNullC(cib), "CIB? as? IC")
    assertEquals(d, nullToNotNullC(d), "D? as? IC")
    assertEquals(null, nullToNotNullC(e), "E? as? IC")
    assertEquals(f, nullToNotNullC(f), "F? as? IC")
    assertEquals(g, nullToNotNullC(g), "G? as? IC")
    assertEquals(null, nullToNotNullC(an), "Any? as? IC")

    assertEquals(null, nullToNotNullE(null), "null as? IE")
    assertEquals(null, nullToNotNullE(a), "A? as? IE")
    assertEquals(null, nullToNotNullE(aia), "AIA? as? IE")
    assertEquals(null, nullToNotNullE(aib), "AIB? as? IE")
    assertEquals(aie, nullToNotNullE(aie), "AIE? as? IE")
    assertEquals(aif, nullToNotNullE(aif), "AIF? as? IE")
    assertEquals(aij, nullToNotNullE(aij), "AIJ? as? IE")
    assertEquals(null, nullToNotNullE(baia), "BAIA? as? IE")
    assertEquals(baiaif, nullToNotNullE(baiaif), "BAIAIF? as? IE")
    assertEquals(baiaij, nullToNotNullE(baiaij), "BAIAIJ? as? IE")
    assertEquals(null, nullToNotNullE(c), "C? as? IE")
    assertEquals(null, nullToNotNullE(cib), "CIB? as? IE")
    assertEquals(d, nullToNotNullE(d), "D? as? IE")
    assertEquals(e, nullToNotNullE(e), "E? as? IE")
    assertEquals(null, nullToNotNullE(f), "F? as? IE")
    assertEquals(g, nullToNotNullE(g), "G? as? IE")
    assertEquals(null, nullToNotNullE(an), "Any? as? IE")

    assertEquals(null, nullToNotNullI(null), "null as? II")
    assertEquals(null, nullToNotNullI(a), "A? as? II")
    assertEquals(null, nullToNotNullI(aia), "AIA? as? II")
    assertEquals(null, nullToNotNullI(aib), "AIB? as? II")
    assertEquals(null, nullToNotNullI(aie), "AIE? as? II")
    assertEquals(null, nullToNotNullI(aif), "AIF? as? II")
    assertEquals(aij, nullToNotNullI(aij), "AIJ? as? II")
    assertEquals(null, nullToNotNullI(baia), "BAIA? as? II")
    assertEquals(null, nullToNotNullI(baiaif), "BAIAIF? as? II")
    assertEquals(baiaij, nullToNotNullI(baiaij), "BAIAIJ? as? II")
    assertEquals(null, nullToNotNullI(c), "C? as? II")
    assertEquals(null, nullToNotNullI(cib), "CIB? as? II")
    assertEquals(null, nullToNotNullI(d), "D? as? II")
    assertEquals(null, nullToNotNullI(e), "E? as? II")
    assertEquals(null, nullToNotNullI(f), "F? as? II")
    assertEquals(g, nullToNotNullI(g), "G? as? II")
    assertEquals(null, nullToNotNullI(an), "Any? as? II")

    assertEquals(null, dToB(null), "null dynamic as? IB")

    assertEquals(aib, NBtoB(aib), "IB()? as? IB")
    assertEquals(null, NBtoB(null), "null IB as? IB")

    assertEquals(null, nullToNotNullI({}), "Function? as? II")
    assertEquals(null, nullToNotNullI(true), "Boolean? as? II")
    assertEquals(null, nullToNotNullI(42), "Number? as? II")
    assertEquals(null, nullToNotNullI("String"), "String? as? II")

    return "OK"
}

fun testInterfaceCastNN2N(): String {
    assertEquals(null, notNullToNullB(a), "A as? IB?")
    assertEquals(null, notNullToNullB(aia), "AIA as? IB?")
    assertEquals(aib, notNullToNullB(aib), "AIB as? IB?")
    assertEquals(null, notNullToNullB(aie), "AIE as? IB?")
    assertEquals(aif, notNullToNullB(aif), "AIF as? IB?")
    assertEquals(aij, notNullToNullB(aij), "AIJ as? IB?")
    assertEquals(baia, notNullToNullB(baia), "BAIA as? IB?")
    assertEquals(baiaif, notNullToNullB(baiaif), "BAIAIF as? IB?")
    assertEquals(baiaij, notNullToNullB(baiaij), "BAIAIJ as? IB?")
    assertEquals(null, notNullToNullB(c), "C as? IB?")
    assertEquals(cib, notNullToNullB(cib), "CIB as? IB?")
    assertEquals(d, notNullToNullB(d), "D as? IB?")
    assertEquals(e, notNullToNullB(e), "E as? IB?")
    assertEquals(f, notNullToNullB(f), "F as? IB?")
    assertEquals(g, notNullToNullB(g), "G as? IB?")
    assertEquals(null, notNullToNullB(an), "Any as? IB?")

    assertEquals(null, notNullToNullC(a), "A as? IC?")
    assertEquals(null, notNullToNullC(aia), "AIA as? IC?")
    assertEquals(null, notNullToNullC(aib), "AIB as? IC?")
    assertEquals(null, notNullToNullC(aie), "AIE as? IC?")
    assertEquals(null, notNullToNullC(aif), "AIF as? IC?")
    assertEquals(null, notNullToNullC(aij), "AIJ as? IC?")
    assertEquals(baia, notNullToNullC(baia), "BAIA as? IC?")
    assertEquals(baiaif, notNullToNullC(baiaif), "BAIAIF as? IC?")
    assertEquals(baiaij, notNullToNullC(baiaij), "BAIAIJ as? IC?")
    assertEquals(null, notNullToNullC(c), "C as? IC?")
    assertEquals(null, notNullToNullC(cib), "CIB as? IC?")
    assertEquals(d, notNullToNullC(d), "D as? IC?")
    assertEquals(null, notNullToNullC(e), "E as? IC?")
    assertEquals(f, notNullToNullC(f), "F as? IC?")
    assertEquals(g, notNullToNullC(g), "G as? IC?")
    assertEquals(null, notNullToNullC(an), "Any as? IC?")

    assertEquals(null, notNullToNullE(a), "A as? IE?")
    assertEquals(null, notNullToNullE(aia), "AIA as? IE?")
    assertEquals(null, notNullToNullE(aib), "AIB as? IE?")
    assertEquals(aie, notNullToNullE(aie), "AIE as? IE?")
    assertEquals(aif, notNullToNullE(aif), "AIF as? IE?")
    assertEquals(aij, notNullToNullE(aij), "AIJ as? IE?")
    assertEquals(null, notNullToNullE(baia), "BAIA as? IE?")
    assertEquals(baiaif, notNullToNullE(baiaif), "BAIAIF as? IE?")
    assertEquals(baiaij, notNullToNullE(baiaij), "BAIAIJ as? IE?")
    assertEquals(null, notNullToNullE(c), "C as? IE?")
    assertEquals(null, notNullToNullE(cib), "CIB as? IE?")
    assertEquals(d, notNullToNullE(d), "D as? IE?")
    assertEquals(e, notNullToNullE(e), "E as? IE?")
    assertEquals(null, notNullToNullE(f), "F as? IE?")
    assertEquals(g, notNullToNullE(g), "G as? IE?")
    assertEquals(null, notNullToNullE(an), "Any as? IE?")

    assertEquals(null, notNullToNullI(a), "A as? II?")
    assertEquals(null, notNullToNullI(aia), "AIA as? II?")
    assertEquals(null, notNullToNullI(aib), "AIB as? II?")
    assertEquals(null, notNullToNullI(aie), "AIE as? II?")
    assertEquals(null, notNullToNullI(aif), "AIF as? II?")
    assertEquals(aij, notNullToNullI(aij), "AIJ as? II?")
    assertEquals(null, notNullToNullI(baia), "BAIA as? II?")
    assertEquals(null, notNullToNullI(baiaif), "BAIAIF as? II?")
    assertEquals(baiaij, notNullToNullI(baiaij), "BAIAIJ as? II?")
    assertEquals(null, notNullToNullI(c), "C as? II?")
    assertEquals(null, notNullToNullI(cib), "CIB as? II?")
    assertEquals(null, notNullToNullI(d), "D as? II?")
    assertEquals(null, notNullToNullI(e), "E as? II?")
    assertEquals(null, notNullToNullI(f), "F as? II?")
    assertEquals(g, notNullToNullI(g), "G as? II?")
    assertEquals(null, notNullToNullI(an), "Any as? II?")

    assertEquals(null, dToNB(dyn), "dynamic as? IB?")
    assertEquals(baiaij, dToNB(baiaij), "BAIAIJ dynamic as? IB?")

    assertEquals(aib, BtoNB(aib), "IB() as? IB?")

    assertEquals(null, notNullToNullI({}), "Function as? II?")
    assertEquals(null, notNullToNullI(true), "Boolean as? II?")
    assertEquals(null, notNullToNullI(42), "Number as? II?")
    assertEquals(null, notNullToNullI("String"), "String as? II?")

    return "OK"
}

fun testInterfaceCastNN2NN(): String {
    assertEquals(null, notNullToNotNullB(a), "A as? IB")
    assertEquals(null, notNullToNotNullB(aia), "AIA as? IB")
    assertEquals(aib, notNullToNotNullB(aib), "AIB as? IB")
    assertEquals(null, notNullToNotNullB(aie), "AIE as? IB")
    assertEquals(aif, notNullToNotNullB(aif), "AIF as? IB")
    assertEquals(aij, notNullToNotNullB(aij), "AIJ as? IB")
    assertEquals(baia, notNullToNotNullB(baia), "BAIA as? IB")
    assertEquals(baiaif, notNullToNotNullB(baiaif), "BAIAIF as? IB")
    assertEquals(baiaij, notNullToNotNullB(baiaij), "BAIAIJ as? IB")
    assertEquals(null, notNullToNotNullB(c), "C as? IB")
    assertEquals(cib, notNullToNotNullB(cib), "CIB as? IB")
    assertEquals(d, notNullToNotNullB(d), "D as? IB")
    assertEquals(e, notNullToNotNullB(e), "E as? IB")
    assertEquals(f, notNullToNotNullB(f), "F as? IB")
    assertEquals(g, notNullToNotNullB(g), "G as? IB")
    assertEquals(null, notNullToNotNullB(an), "Any as? IB")

    assertEquals(null, notNullToNotNullC(a), "A as? IC")
    assertEquals(null, notNullToNotNullC(aia), "AIA as? IC")
    assertEquals(null, notNullToNotNullC(aib), "AIB as? IC")
    assertEquals(null, notNullToNotNullC(aie), "AIE as? IC")
    assertEquals(null, notNullToNotNullC(aif), "AIF as? IC")
    assertEquals(null, notNullToNotNullC(aij), "AIJ as? IC")
    assertEquals(baia, notNullToNotNullC(baia), "BAIA as? IC")
    assertEquals(baiaif, notNullToNotNullC(baiaif), "BAIAIF as? IC")
    assertEquals(baiaij, notNullToNotNullC(baiaij), "BAIAIJ as? IC")
    assertEquals(null, notNullToNotNullC(c), "C as? IC")
    assertEquals(null, notNullToNotNullC(cib), "CIB as? IC")
    assertEquals(d, notNullToNotNullC(d), "D as? IC")
    assertEquals(null, notNullToNotNullC(e), "E as? IC")
    assertEquals(f, notNullToNotNullC(f), "F as? IC")
    assertEquals(g, notNullToNotNullC(g), "G as? IC")
    assertEquals(null, notNullToNotNullC(an), "Any as? IC")

    assertEquals(null, notNullToNotNullE(a), "A as? IE")
    assertEquals(null, notNullToNotNullE(aia), "AIA as? IE")
    assertEquals(null, notNullToNotNullE(aib), "AIB as? IE")
    assertEquals(aie, notNullToNotNullE(aie), "AIE as? IE")
    assertEquals(aif, notNullToNotNullE(aif), "AIF as? IE")
    assertEquals(aij, notNullToNotNullE(aij), "AIJ as? IE")
    assertEquals(null, notNullToNotNullE(baia), "BAIA as? IE")
    assertEquals(baiaif, notNullToNotNullE(baiaif), "BAIAIF as? IE")
    assertEquals(baiaij, notNullToNotNullE(baiaij), "BAIAIJ as? IE")
    assertEquals(null, notNullToNotNullE(c), "C as? IE")
    assertEquals(null, notNullToNotNullE(cib), "CIB as? IE")
    assertEquals(d, notNullToNotNullE(d), "D as? IE")
    assertEquals(e, notNullToNotNullE(e), "E as? IE")
    assertEquals(null, notNullToNotNullE(f), "F as? IE")
    assertEquals(g, notNullToNotNullE(g), "G as? IE")
    assertEquals(null, notNullToNotNullE(an), "Any as? IE")

    assertEquals(null, notNullToNotNullI(a), "A as? II")
    assertEquals(null, notNullToNotNullI(aia), "AIA as? II")
    assertEquals(null, notNullToNotNullI(aib), "AIB as? II")
    assertEquals(null, notNullToNotNullI(aie), "AIE as? II")
    assertEquals(null, notNullToNotNullI(aif), "AIF as? II")
    assertEquals(aij, notNullToNotNullI(aij), "AIJ as? II")
    assertEquals(null, notNullToNotNullI(baia), "BAIA as? II")
    assertEquals(null, notNullToNotNullI(baiaif), "BAIAIF as? II")
    assertEquals(baiaij, notNullToNotNullI(baiaij), "BAIAIJ as? II")
    assertEquals(null, notNullToNotNullI(c), "C as? II")
    assertEquals(null, notNullToNotNullI(cib), "CIB as? II")
    assertEquals(null, notNullToNotNullI(d), "D as? II")
    assertEquals(null, notNullToNotNullI(e), "E as? II")
    assertEquals(null, notNullToNotNullI(f), "F as? II")
    assertEquals(g, notNullToNotNullI(g), "G as? II")
    assertEquals(null, notNullToNotNullI(an), "Any as? II")

    assertEquals(null, dToB(dyn), "dynamic as? IB")
    assertEquals(baiaij, dToB(baiaij), "BAIAIJ dynamic as? IB")

    assertEquals(null, notNullToNotNullI({}), "Function as? II")
    assertEquals(null, notNullToNotNullI(true), "Boolean as? II")
    assertEquals(null, notNullToNotNullI(42), "Number as? II")
    assertEquals(null, notNullToNotNullI("String"), "String as? II")

    return "OK"
}

fun testInterfaceCast() {
    assertEquals("OK", testInterfaceCastN2N())
    assertEquals("OK", testInterfaceCastN2NN())
    assertEquals("OK", testInterfaceCastNN2N())
    assertEquals("OK", testInterfaceCastNN2NN())
}


fun box(): String {
    testInterfaceCast()
    return "OK"
}