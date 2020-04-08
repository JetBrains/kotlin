// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun foo() {
    var x = "a"
    x = "b"
}
