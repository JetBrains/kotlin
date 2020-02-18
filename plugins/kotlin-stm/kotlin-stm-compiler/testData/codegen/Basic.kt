// CURIOUS_ABOUT f, <init>, <clinit>
// WITH_RUNTIME

package koko

import kotlinx.stm.*

@SharedMutable
class User(val firstName: String, val lastName: String) {
//    val stm: STM = STMSearcher.findStm()

    fun f() {
        // return this.stm.runAtomically(null) { }
    }
}

//class KoUser(val firstName: String, val lastName: String) {
//    fun ko() = 5
//}