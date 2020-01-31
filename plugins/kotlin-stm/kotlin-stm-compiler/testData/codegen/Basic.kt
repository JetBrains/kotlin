// CURIOUS_ABOUT publish, deserialize <init>, <clinit>
// WITH_RUNTIME

package koko

import kotlinx.stm.*

@SharedMutable
class User(val firstName: String, val lastName: String) {
//    val stm: STM = STMSearcher.findStm()
    class UNested(val xn: Int)

    fun f() {
        // return stm.runAtomically(null) { }
    }
}

//class KoUser(val firstName: String, val lastName: String) {
//    fun ko() = 5
//}