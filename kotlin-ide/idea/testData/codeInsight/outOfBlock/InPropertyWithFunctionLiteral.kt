// OUT_OF_CODE_BLOCK: TRUE
// ERROR: Unresolved reference: apri
// SKIP_ANALYZE_CHECK

class Test {
    val a : () -> Int = { <caret>pri }
}