// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: ILLEGAL_SUSPEND_FUNCTION_CALL
suspend fun foo() {}

fun noSuspend() {
    foo()
}
