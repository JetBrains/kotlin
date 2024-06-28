// WITH_EXTENDED_CHECKERS
// ISSUE: KT-54496

annotation class AllOpen

@AllOpen
class Some {
    fun default() {}
    <!REDUNDANT_MODALITY_MODIFIER!>open<!> fun meaninglessOpen() {}
    final fun meaningfullFinal() {}
}
