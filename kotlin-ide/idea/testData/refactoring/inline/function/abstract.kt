// ERROR: Inline Function refactoring cannot be applied to abstract function

interface SilentFace { fun hush() }
fun callSilently(pf: SilentFace) {
    pf.hu<caret>sh()
}