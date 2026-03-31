// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

sealed interface BaseObjIface
object IObj1 : BaseObjIface { val x = 1 }
object IObj2 : BaseObjIface { val x = 2 }

// SNIPPET

sealed class BaseObjClass(val y: Int)
object Obj1 : BaseObjClass(11)
object Obj2 : BaseObjClass(12)

// SNIPPET

val resi1 = <!UNRESOLVED_REFERENCE!>IObj1<!>.x
val res2 = <!UNRESOLVED_REFERENCE!>Obj2<!>.y

// EXPECTED: resi1 == 1
// EXPECTED: res2 == 12

// SNIPPET

object IObj3 : <!UNRESOLVED_REFERENCE!>BaseObjIface<!> { val x = 3 }
object Obj3 : <!UNRESOLVED_REFERENCE!>BaseObjClass<!>(13)

// SNIPPET

val resi3 = <!UNRESOLVED_REFERENCE!>IObj3<!>.x
val res3 = <!UNRESOLVED_REFERENCE!>Obj3<!>.y

// EXPECTED: resi3 == 3
// EXPECTED: res3 == 13
