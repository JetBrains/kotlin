package first

import util.*

fun test() {
    topLevelFun(topLevelVar)
    topLevelFun(topLevelVal)
    val c = C("ff", 1)
    c.s.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: invalid">invalid</error>
    val <warning descr="[UNUSED_VARIABLE] Variable 'b' is never used">b</warning> = B()
    funWithVararg(1, 2, 3)
    val <warning descr="[UNUSED_VARIABLE] Variable 'i' is never used">i</warning> = Invalid()
    topLevelObject.f()
    topLevelObject.g()
 }

fun testWhere(list: List<Int>) {
    funWithWhere(1, list)
    funWithWhere(1, <error descr="[CONSTANT_EXPECTED_TYPE_MISMATCH] The integer literal does not conform to the expected type Collection<TypeVariable(G)>">2</error>)
}

class Example : C("foo", 0)
class Example2 : <error descr="[FINAL_SUPERTYPE] This type is final, so it cannot be inherited from">A</error>(2)
