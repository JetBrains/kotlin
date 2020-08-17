// ERROR: Inline Property refactoring cannot be applied to abstract property

abstract class AbstractSilence { abstract val <caret>hush: Unit }
fun callSilently(pac: AbstractSilence) {
    pac.hush
}