// REMOVE_COMPANION_REF
package test

class CompanionExtraPlain { companion object { val cmpVal = 1 } }

fun wrap(p: Int) = p + 1

class Dome {
    fun refer() {
        <selection>test.wrap(test.CompanionExtraPlain.Companion.cmpVal)</selection>
    }
}