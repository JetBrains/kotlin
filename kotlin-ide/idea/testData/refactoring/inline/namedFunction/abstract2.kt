// ERROR: Inline Function refactoring cannot be applied to abstract declaration

abstract class AbstractSilence { abstract fun <caret>hush() }
fun callSilently(pac: AbstractSilence) {
    pac.hush()
}