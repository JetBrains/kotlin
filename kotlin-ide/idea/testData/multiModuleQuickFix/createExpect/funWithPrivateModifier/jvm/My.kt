// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: "The declaration has `private` modifier"
// DISABLE-ERRORS

private actual fun <caret>s() = "s"