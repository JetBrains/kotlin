package transitiveStory.bottomActual.intermediateSrc

import transitiveStory.bottomActual.mppBeginning.MPOuter
import transitiveStory.bottomActual.mppBeginning.Outer
import transitiveStory.bottomActual.mppBeginning.tlInternalInCommon

//import transitiveStory.bottomActual.mppBeginning.tlInternalInCommon

class InBottomActualIntermediate {
    val p = 42
    // https://youtrack.jetbrains.com/issue/KT-37264
    val callingInteral = tlInternalInCommon
}

expect class IntermediateMPPClassInBottomActual()


class Subclass : Outer() {
    // a is not visible
    // b, c and d are visible
    // Nested and e are visible

    override val b = 5   // 'b' is protected
}

class ChildOfCommonInShared : Outer() {
    override val b: Int
        get() = super.b + 243
//    val callAlso = super.c // internal in Outer

    private val other = Nested()
}

class ChildOfMPOuterInShared : MPOuter() {
    private val sav = MPNested()
}

