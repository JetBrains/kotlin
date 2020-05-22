// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break
// DISABLE-ERRORS

fun a() {
    g[<caret>1, 2, 3,/**/
    ]
}