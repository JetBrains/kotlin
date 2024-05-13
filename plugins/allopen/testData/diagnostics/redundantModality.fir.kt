// WITH_EXTENDED_CHECKERS
// ISSUE: KT-54496

annotation class AllOpen

@AllOpen
class Some {
    fun default() {}
    open fun meaninglessOpen() {}
    <!REDUNDANT_MODALITY_MODIFIER!>final<!> fun meaningfullFinal() {}
}
