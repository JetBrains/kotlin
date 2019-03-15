// EXPECTED_REACHABLE_NODES: 1403
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

var dyn = js("({})")

fun dToNB(d: dynamic) = d is IB?
fun dToB(d: dynamic) = d is IB

fun NBtoB(b: IB?) = b is IB
fun BtoNB(b: IB) = b is IB?

fun nullToNullB(a: Any?) = a is IB?
fun notNullToNullB(a: Any) = a is IB?
fun nullToNotNullB(a: Any?) = a is IB
fun notNullToNotNullB(a: Any) = a is IB

fun nullToNullC(a: Any?) = a is IC?
fun notNullToNullC(a: Any) = a is IC?
fun nullToNotNullC(a: Any?) = a is IC
fun notNullToNotNullC(a: Any) = a is IC

fun nullToNullE(a: Any?) = a is IE?
fun notNullToNullE(a: Any) = a is IE?
fun nullToNotNullE(a: Any?) = a is IE
fun notNullToNotNullE(a: Any) = a is IE

fun nullToNullI(a: Any?) = a is II?
fun notNullToNullI(a: Any) = a is II?
fun nullToNotNullI(a: Any?) = a is II
fun notNullToNotNullI(a: Any) = a is II

fun testInterfaceCastN2N(): String {
    assertEquals(true, nullToNullB(null), "null is IB?")
    assertEquals(false, nullToNullB(A()), "A? is IB?")
    assertEquals(false, nullToNullB(AIA()), "AIA? is IB?")
    assertEquals(true, nullToNullB(AIB()), "AIB? is IB?")
    assertEquals(false, nullToNullB(AIE()), "AIE? is IB?")
    assertEquals(true, nullToNullB(AIF()), "AIF? is IB?")
    assertEquals(true, nullToNullB(AIJ()), "AIJ? is IB?")
    assertEquals(true, nullToNullB(BAIA()), "BAIA? is IB?")
    assertEquals(true, nullToNullB(BAIAIF()), "BAIAIF? is IB?")
    assertEquals(true, nullToNullB(BAIAIJ()), "BAIAIJ? is IB?")
    assertEquals(false, nullToNullB(C()), "C? is IB?")
    assertEquals(true, nullToNullB(CIB()), "CIB? is IB?")
    assertEquals(true, nullToNullB(D()), "D? is IB?")
    assertEquals(true, nullToNullB(E()), "E? is IB?")
    assertEquals(true, nullToNullB(F()), "F? is IB?")
    assertEquals(true, nullToNullB(G()), "G? is IB?")
    assertEquals(false, nullToNullB(Any()), "Any? is IB?")

    assertEquals(true, nullToNullC(null), "null is IC?")
    assertEquals(false, nullToNullC(A()), "A? is IC?")
    assertEquals(false, nullToNullC(AIA()), "AIA? is IC?")
    assertEquals(false, nullToNullC(AIB()), "AIB? is IC?")
    assertEquals(false, nullToNullC(AIE()), "AIE? is IC?")
    assertEquals(false, nullToNullC(AIF()), "AIF? is IC?")
    assertEquals(false, nullToNullC(AIJ()), "AIJ? is IC?")
    assertEquals(true, nullToNullC(BAIA()), "BAIA? is IC?")
    assertEquals(true, nullToNullC(BAIAIF()), "BAIAIF? is IC?")
    assertEquals(true, nullToNullC(BAIAIJ()), "BAIAIJ? is IC?")
    assertEquals(false, nullToNullC(C()), "C? is IC?")
    assertEquals(false, nullToNullC(CIB()), "CIB? is IC?")
    assertEquals(true, nullToNullC(D()), "D? is IC?")
    assertEquals(false, nullToNullC(E()), "E? is IC?")
    assertEquals(true, nullToNullC(F()), "F? is IC?")
    assertEquals(true, nullToNullC(G()), "G? is IC?")
    assertEquals(false, nullToNullC(Any()), "Any? is IC?")

    assertEquals(true, nullToNullE(null), "null is IE?")
    assertEquals(false, nullToNullE(A()), "A? is IE?")
    assertEquals(false, nullToNullE(AIA()), "AIA? is IE?")
    assertEquals(false, nullToNullE(AIB()), "AIB? is IE?")
    assertEquals(true, nullToNullE(AIE()), "AIE? is IE?")
    assertEquals(true, nullToNullE(AIF()), "AIF? is IE?")
    assertEquals(true, nullToNullE(AIJ()), "AIJ? is IE?")
    assertEquals(false, nullToNullE(BAIA()), "BAIA? is IE?")
    assertEquals(true, nullToNullE(BAIAIF()), "BAIAIF? is IE?")
    assertEquals(true, nullToNullE(BAIAIJ()), "BAIAIJ? is IE?")
    assertEquals(false, nullToNullE(C()), "C? is IE?")
    assertEquals(false, nullToNullE(CIB()), "CIB? is IE?")
    assertEquals(true, nullToNullE(D()), "D? is IE?")
    assertEquals(true, nullToNullE(E()), "E? is IE?")
    assertEquals(false, nullToNullE(F()), "F? is IE?")
    assertEquals(true, nullToNullE(G()), "G? is IE?")
    assertEquals(false, nullToNullE(Any()), "Any? is IE?")

    assertEquals(true, nullToNullI(null), "null is II?")
    assertEquals(false, nullToNullI(A()), "A? is II?")
    assertEquals(false, nullToNullI(AIA()), "AIA? is II?")
    assertEquals(false, nullToNullI(AIB()), "AIB? is II?")
    assertEquals(false, nullToNullI(AIE()), "AIE? is II?")
    assertEquals(false, nullToNullI(AIF()), "AIF? is II?")
    assertEquals(true, nullToNullI(AIJ()), "AIJ? is II?")
    assertEquals(false, nullToNullI(BAIA()), "BAIA? is II?")
    assertEquals(false, nullToNullI(BAIAIF()), "BAIAIF? is II?")
    assertEquals(true, nullToNullI(BAIAIJ()), "BAIAIJ? is II?")
    assertEquals(false, nullToNullI(C()), "C? is II?")
    assertEquals(false, nullToNullI(CIB()), "CIB? is II?")
    assertEquals(false, nullToNullI(D()), "D? is II?")
    assertEquals(false, nullToNullI(E()), "E? is II?")
    assertEquals(false, nullToNullI(F()), "F? is II?")
    assertEquals(true, nullToNullI(G()), "G? is II?")
    assertEquals(false, nullToNullI(Any()), "Any? is II?")

    assertEquals(true, dToNB(null), "null dynamic is IB?")

    assertEquals(false, nullToNullI({}), "Function? is II?")
    assertEquals(false, nullToNullI(true), "Boolean? is II?")
    assertEquals(false, nullToNullI(42), "Number? is II?")
    assertEquals(false, nullToNullI("String"), "String? is II?")

    return "OK"
}

fun testInterfaceCastN2NN(): String {

    assertEquals(false, nullToNotNullB(null), "null is IB")
    assertEquals(false, nullToNotNullB(A()), "A? is IB")
    assertEquals(false, nullToNotNullB(AIA()), "AIA? is IB")
    assertEquals(true, nullToNotNullB(AIB()), "AIB? is IB")
    assertEquals(false, nullToNotNullB(AIE()), "AIE? is IB")
    assertEquals(true, nullToNotNullB(AIF()), "AIF? is IB")
    assertEquals(true, nullToNotNullB(AIJ()), "AIJ? is IB")
    assertEquals(true, nullToNotNullB(BAIA()), "BAIA? is IB")
    assertEquals(true, nullToNotNullB(BAIAIF()), "BAIAIF? is IB")
    assertEquals(true, nullToNotNullB(BAIAIJ()), "BAIAIJ? is IB")
    assertEquals(false, nullToNotNullB(C()), "C? is IB")
    assertEquals(true, nullToNotNullB(CIB()), "CIB? is IB")
    assertEquals(true, nullToNotNullB(D()), "D? is IB")
    assertEquals(true, nullToNotNullB(E()), "E? is IB")
    assertEquals(true, nullToNotNullB(F()), "F? is IB")
    assertEquals(true, nullToNotNullB(G()), "G? is IB")
    assertEquals(false, nullToNotNullB(Any()), "Any? is IB")

    assertEquals(false, nullToNotNullC(null), "null is IC")
    assertEquals(false, nullToNotNullC(A()), "A? is IC")
    assertEquals(false, nullToNotNullC(AIA()), "AIA? is IC")
    assertEquals(false, nullToNotNullC(AIB()), "AIB? is IC")
    assertEquals(false, nullToNotNullC(AIE()), "AIE? is IC")
    assertEquals(false, nullToNotNullC(AIF()), "AIF? is IC")
    assertEquals(false, nullToNotNullC(AIJ()), "AIJ? is IC")
    assertEquals(true, nullToNotNullC(BAIA()), "BAIA? is IC")
    assertEquals(true, nullToNotNullC(BAIAIF()), "BAIAIF? is IC")
    assertEquals(true, nullToNotNullC(BAIAIJ()), "BAIAIJ? is IC")
    assertEquals(false, nullToNotNullC(C()), "C? is IC")
    assertEquals(false, nullToNotNullC(CIB()), "CIB? is IC")
    assertEquals(true, nullToNotNullC(D()), "D? is IC")
    assertEquals(false, nullToNotNullC(E()), "E? is IC")
    assertEquals(true, nullToNotNullC(F()), "F? is IC")
    assertEquals(true, nullToNotNullC(G()), "G? is IC")
    assertEquals(false, nullToNotNullC(Any()), "Any? is IC")

    assertEquals(false, nullToNotNullE(null), "null is IE")
    assertEquals(false, nullToNotNullE(A()), "A? is IE")
    assertEquals(false, nullToNotNullE(AIA()), "AIA? is IE")
    assertEquals(false, nullToNotNullE(AIB()), "AIB? is IE")
    assertEquals(true, nullToNotNullE(AIE()), "AIE? is IE")
    assertEquals(true, nullToNotNullE(AIF()), "AIF? is IE")
    assertEquals(true, nullToNotNullE(AIJ()), "AIJ? is IE")
    assertEquals(false, nullToNotNullE(BAIA()), "BAIA? is IE")
    assertEquals(true, nullToNotNullE(BAIAIF()), "BAIAIF? is IE")
    assertEquals(true, nullToNotNullE(BAIAIJ()), "BAIAIJ? is IE")
    assertEquals(false, nullToNotNullE(C()), "C? is IE")
    assertEquals(false, nullToNotNullE(CIB()), "CIB? is IE")
    assertEquals(true, nullToNotNullE(D()), "D? is IE")
    assertEquals(true, nullToNotNullE(E()), "E? is IE")
    assertEquals(false, nullToNotNullE(F()), "F? is IE")
    assertEquals(true, nullToNotNullE(G()), "G? is IE")
    assertEquals(false, nullToNotNullE(Any()), "Any? is IE")

    assertEquals(false, nullToNotNullI(null), "null is II")
    assertEquals(false, nullToNotNullI(A()), "A? is II")
    assertEquals(false, nullToNotNullI(AIA()), "AIA? is II")
    assertEquals(false, nullToNotNullI(AIB()), "AIB? is II")
    assertEquals(false, nullToNotNullI(AIE()), "AIE? is II")
    assertEquals(false, nullToNotNullI(AIF()), "AIF? is II")
    assertEquals(true, nullToNotNullI(AIJ()), "AIJ? is II")
    assertEquals(false, nullToNotNullI(BAIA()), "BAIA? is II")
    assertEquals(false, nullToNotNullI(BAIAIF()), "BAIAIF? is II")
    assertEquals(true, nullToNotNullI(BAIAIJ()), "BAIAIJ? is II")
    assertEquals(false, nullToNotNullI(C()), "C? is II")
    assertEquals(false, nullToNotNullI(CIB()), "CIB? is II")
    assertEquals(false, nullToNotNullI(D()), "D? is II")
    assertEquals(false, nullToNotNullI(E()), "E? is II")
    assertEquals(false, nullToNotNullI(F()), "F? is II")
    assertEquals(true, nullToNotNullI(G()), "G? is II")
    assertEquals(false, nullToNotNullI(Any()), "Any? is II")

    assertEquals(false, dToB(null), "null dynamic is IB")

    assertEquals(true, NBtoB(AIB()), "IB()? is IB")
    assertEquals(false, NBtoB(null), "null IB is IB")

    assertEquals(false, nullToNotNullI({}), "Function? is II")
    assertEquals(false, nullToNotNullI(true), "Boolean? is II")
    assertEquals(false, nullToNotNullI(42), "Number? is II")
    assertEquals(false, nullToNotNullI("String"), "String? is II")

    return "OK"
}

fun testInterfaceCastNN2N(): String {
    assertEquals(false, notNullToNullB(A()), "A is IB?")
    assertEquals(false, notNullToNullB(AIA()), "AIA is IB?")
    assertEquals(true, notNullToNullB(AIB()), "AIB is IB?")
    assertEquals(false, notNullToNullB(AIE()), "AIE is IB?")
    assertEquals(true, notNullToNullB(AIF()), "AIF is IB?")
    assertEquals(true, notNullToNullB(AIJ()), "AIJ is IB?")
    assertEquals(true, notNullToNullB(BAIA()), "BAIA is IB?")
    assertEquals(true, notNullToNullB(BAIAIF()), "BAIAIF is IB?")
    assertEquals(true, notNullToNullB(BAIAIJ()), "BAIAIJ is IB?")
    assertEquals(false, notNullToNullB(C()), "C is IB?")
    assertEquals(true, notNullToNullB(CIB()), "CIB is IB?")
    assertEquals(true, notNullToNullB(D()), "D is IB?")
    assertEquals(true, notNullToNullB(E()), "E is IB?")
    assertEquals(true, notNullToNullB(F()), "F is IB?")
    assertEquals(true, notNullToNullB(G()), "G is IB?")
    assertEquals(false, notNullToNullB(Any()), "Any is IB?")

    assertEquals(false, notNullToNullC(A()), "A is IC?")
    assertEquals(false, notNullToNullC(AIA()), "AIA is IC?")
    assertEquals(false, notNullToNullC(AIB()), "AIB is IC?")
    assertEquals(false, notNullToNullC(AIE()), "AIE is IC?")
    assertEquals(false, notNullToNullC(AIF()), "AIF is IC?")
    assertEquals(false, notNullToNullC(AIJ()), "AIJ is IC?")
    assertEquals(true, notNullToNullC(BAIA()), "BAIA is IC?")
    assertEquals(true, notNullToNullC(BAIAIF()), "BAIAIF is IC?")
    assertEquals(true, notNullToNullC(BAIAIJ()), "BAIAIJ is IC?")
    assertEquals(false, notNullToNullC(C()), "C is IC?")
    assertEquals(false, notNullToNullC(CIB()), "CIB is IC?")
    assertEquals(true, notNullToNullC(D()), "D is IC?")
    assertEquals(false, notNullToNullC(E()), "E is IC?")
    assertEquals(true, notNullToNullC(F()), "F is IC?")
    assertEquals(true, notNullToNullC(G()), "G is IC?")
    assertEquals(false, notNullToNullC(Any()), "Any is IC?")

    assertEquals(false, notNullToNullE(A()), "A is IE?")
    assertEquals(false, notNullToNullE(AIA()), "AIA is IE?")
    assertEquals(false, notNullToNullE(AIB()), "AIB is IE?")
    assertEquals(true, notNullToNullE(AIE()), "AIE is IE?")
    assertEquals(true, notNullToNullE(AIF()), "AIF is IE?")
    assertEquals(true, notNullToNullE(AIJ()), "AIJ is IE?")
    assertEquals(false, notNullToNullE(BAIA()), "BAIA is IE?")
    assertEquals(true, notNullToNullE(BAIAIF()), "BAIAIF is IE?")
    assertEquals(true, notNullToNullE(BAIAIJ()), "BAIAIJ is IE?")
    assertEquals(false, notNullToNullE(C()), "C is IE?")
    assertEquals(false, notNullToNullE(CIB()), "CIB is IE?")
    assertEquals(true, notNullToNullE(D()), "D is IE?")
    assertEquals(true, notNullToNullE(E()), "E is IE?")
    assertEquals(false, notNullToNullE(F()), "F is IE?")
    assertEquals(true, notNullToNullE(G()), "G is IE?")
    assertEquals(false, notNullToNullE(Any()), "Any is IE?")

    assertEquals(false, notNullToNullI(A()), "A is II?")
    assertEquals(false, notNullToNullI(AIA()), "AIA is II?")
    assertEquals(false, notNullToNullI(AIB()), "AIB is II?")
    assertEquals(false, notNullToNullI(AIE()), "AIE is II?")
    assertEquals(false, notNullToNullI(AIF()), "AIF is II?")
    assertEquals(true, notNullToNullI(AIJ()), "AIJ is II?")
    assertEquals(false, notNullToNullI(BAIA()), "BAIA is II?")
    assertEquals(false, notNullToNullI(BAIAIF()), "BAIAIF is II?")
    assertEquals(true, notNullToNullI(BAIAIJ()), "BAIAIJ is II?")
    assertEquals(false, notNullToNullI(C()), "C is II?")
    assertEquals(false, notNullToNullI(CIB()), "CIB is II?")
    assertEquals(false, notNullToNullI(D()), "D is II?")
    assertEquals(false, notNullToNullI(E()), "E is II?")
    assertEquals(false, notNullToNullI(F()), "F is II?")
    assertEquals(true, notNullToNullI(G()), "G is II?")
    assertEquals(false, notNullToNullI(Any()), "Any is II?")

    assertEquals(false, dToNB(dyn), "dynamic is IB?")
    assertEquals(true, dToNB(BAIAIJ()), "BAIAIJ dynamic is IB?")

    assertEquals(true, BtoNB(AIB()), "IB() is IB?")

    assertEquals(false, notNullToNullI({}), "Function is II?")
    assertEquals(false, notNullToNullI(true), "Boolean is II?")
    assertEquals(false, notNullToNullI(42), "Number is II?")
    assertEquals(false, notNullToNullI("String"), "String is II?")

    return "OK"
}

fun testInterfaceCastNN2NN(): String {
    assertEquals(false, notNullToNotNullB(A()), "A is IB")
    assertEquals(false, notNullToNotNullB(AIA()), "AIA is IB")
    assertEquals(true, notNullToNotNullB(AIB()), "AIB is IB")
    assertEquals(false, notNullToNotNullB(AIE()), "AIE is IB")
    assertEquals(true, notNullToNotNullB(AIF()), "AIF is IB")
    assertEquals(true, notNullToNotNullB(AIJ()), "AIJ is IB")
    assertEquals(true, notNullToNotNullB(BAIA()), "BAIA is IB")
    assertEquals(true, notNullToNotNullB(BAIAIF()), "BAIAIF is IB")
    assertEquals(true, notNullToNotNullB(BAIAIJ()), "BAIAIJ is IB")
    assertEquals(false, notNullToNotNullB(C()), "C is IB")
    assertEquals(true, notNullToNotNullB(CIB()), "CIB is IB")
    assertEquals(true, notNullToNotNullB(D()), "D is IB")
    assertEquals(true, notNullToNotNullB(E()), "E is IB")
    assertEquals(true, notNullToNotNullB(F()), "F is IB")
    assertEquals(true, notNullToNotNullB(G()), "G is IB")
    assertEquals(false, notNullToNotNullB(Any()), "Any is IB")

    assertEquals(false, notNullToNotNullC(A()), "A is IC")
    assertEquals(false, notNullToNotNullC(AIA()), "AIA is IC")
    assertEquals(false, notNullToNotNullC(AIB()), "AIB is IC")
    assertEquals(false, notNullToNotNullC(AIE()), "AIE is IC")
    assertEquals(false, notNullToNotNullC(AIF()), "AIF is IC")
    assertEquals(false, notNullToNotNullC(AIJ()), "AIJ is IC")
    assertEquals(true, notNullToNotNullC(BAIA()), "BAIA is IC")
    assertEquals(true, notNullToNotNullC(BAIAIF()), "BAIAIF is IC")
    assertEquals(true, notNullToNotNullC(BAIAIJ()), "BAIAIJ is IC")
    assertEquals(false, notNullToNotNullC(C()), "C is IC")
    assertEquals(false, notNullToNotNullC(CIB()), "CIB is IC")
    assertEquals(true, notNullToNotNullC(D()), "D is IC")
    assertEquals(false, notNullToNotNullC(E()), "E is IC")
    assertEquals(true, notNullToNotNullC(F()), "F is IC")
    assertEquals(true, notNullToNotNullC(G()), "G is IC")
    assertEquals(false, notNullToNotNullC(Any()), "Any is IC")

    assertEquals(false, notNullToNotNullE(A()), "A is IE")
    assertEquals(false, notNullToNotNullE(AIA()), "AIA is IE")
    assertEquals(false, notNullToNotNullE(AIB()), "AIB is IE")
    assertEquals(true, notNullToNotNullE(AIE()), "AIE is IE")
    assertEquals(true, notNullToNotNullE(AIF()), "AIF is IE")
    assertEquals(true, notNullToNotNullE(AIJ()), "AIJ is IE")
    assertEquals(false, notNullToNotNullE(BAIA()), "BAIA is IE")
    assertEquals(true, notNullToNotNullE(BAIAIF()), "BAIAIF is IE")
    assertEquals(true, notNullToNotNullE(BAIAIJ()), "BAIAIJ is IE")
    assertEquals(false, notNullToNotNullE(C()), "C is IE")
    assertEquals(false, notNullToNotNullE(CIB()), "CIB is IE")
    assertEquals(true, notNullToNotNullE(D()), "D is IE")
    assertEquals(true, notNullToNotNullE(E()), "E is IE")
    assertEquals(false, notNullToNotNullE(F()), "F is IE")
    assertEquals(true, notNullToNotNullE(G()), "G is IE")
    assertEquals(false, notNullToNotNullE(Any()), "Any is IE")

    assertEquals(false, notNullToNotNullI(A()), "A is II")
    assertEquals(false, notNullToNotNullI(AIA()), "AIA is II")
    assertEquals(false, notNullToNotNullI(AIB()), "AIB is II")
    assertEquals(false, notNullToNotNullI(AIE()), "AIE is II")
    assertEquals(false, notNullToNotNullI(AIF()), "AIF is II")
    assertEquals(true, notNullToNotNullI(AIJ()), "AIJ is II")
    assertEquals(false, notNullToNotNullI(BAIA()), "BAIA is II")
    assertEquals(false, notNullToNotNullI(BAIAIF()), "BAIAIF is II")
    assertEquals(true, notNullToNotNullI(BAIAIJ()), "BAIAIJ is II")
    assertEquals(false, notNullToNotNullI(C()), "C is II")
    assertEquals(false, notNullToNotNullI(CIB()), "CIB is II")
    assertEquals(false, notNullToNotNullI(D()), "D is II")
    assertEquals(false, notNullToNotNullI(E()), "E is II")
    assertEquals(false, notNullToNotNullI(F()), "F is II")
    assertEquals(true, notNullToNotNullI(G()), "G is II")
    assertEquals(false, notNullToNotNullI(Any()), "Any is II")

    assertEquals(false, dToB(dyn), "dynamic is IB")
    assertEquals(true, dToB(BAIAIJ()), "BAIAIJ dynamic is IB")

    assertEquals(false, notNullToNotNullI({}), "Function is II")
    assertEquals(false, notNullToNotNullI(true), "Boolean is II")
    assertEquals(false, notNullToNotNullI(42), "Number is II")
    assertEquals(false, notNullToNotNullI("String"), "String is II")

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