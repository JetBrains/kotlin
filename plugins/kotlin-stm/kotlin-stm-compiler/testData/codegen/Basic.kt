// CURIOUS_ABOUT <init>, invoke, g, f, _get_firstName$_______Sharable____, _get_lastName$_______Sharable____, _set_firstName$_______Sharable____, _set_lastName$_______Sharable____
// WITH_RUNTIME

package koko

import kotlinx.stm.*

@SharedMutable
class User(fname: String, lname: String) {
    var firstName: String = fname
    var lastName: String = lname
//    val stm: STM = STMSearcher.findStm()
//    val firstName$SHARABLE: UniversalDelegate<String> = stm.wrap(fname)
//    val lastName$SHARABLE: UniversalDelegate<String> = stm.wrap(lname)

//    fun _set_firstName$SHARABLE(ctx: STMContext, newValue: String) { stm.setVar(ctx, firstName$SHARABLE, newValue) }
//    fun _get_firstName$SHARABLE(ctx: STMContext): String = stm.getVar(ctx, firstName$SHARABLE)

//    fun _set_lastName$SHARABLE(ctx: STMContext, newValue: String) { stm.setVar(ctx, lastName$SHARABLE, newValue) }
//    fun _get_lastName$SHARABLE(ctx: STMContext): String = stm.getVar(ctx, lastName$SHARABLE)

    fun f() {
        // return this.stm.runAtomically(null) { }
    }
}

class C {
    fun g() {
        val u = User("Vadim", "Briliantov")

        runAtomically {
            val tmp = u.firstName
            u.firstName = u.lastName
            u.lastName = tmp
        }
    }
}

//class KoUser(val firstName: String, val lastName: String) {
//    fun ko() = 5
//}