// ERROR: Inline Property refactoring cannot be applied to abstract property

interface SilentFace { val hush: Unit }
fun callSilently(pf: SilentFace) {
    pf.hu<caret>sh
}