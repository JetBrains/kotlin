// SNIPPET

sealed interface BaseObjIface
object IObj1 : BaseObjIface { val x = 1 }
object IObj2 : BaseObjIface { val x = 2 }

// SNIPPET

sealed class BaseObjClass(val y: Int)
object Obj1 : BaseObjClass(11)
object Obj2 : BaseObjClass(12)

// SNIPPET

val resi1 = IObj1.x
val res2 = Obj2.y

// EXPECTED: resi1 == 1
// EXPECTED: res2 == 12

// SNIPPET

object IObj3 : BaseObjIface { val x = 3 }
object Obj3 : BaseObjClass(13)

// SNIPPET

val resi3 = IObj3.x
val res3 = Obj3.y

// EXPECTED: resi3 == 3
// EXPECTED: res3 == 13
