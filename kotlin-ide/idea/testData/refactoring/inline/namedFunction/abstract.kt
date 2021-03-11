// ERROR: Inline Function refactoring cannot be applied to abstract declaration

interface SilentFace { fun hush() }
fun callSilently(pf: SilentFace) {
    pf.hu<caret>sh()
}