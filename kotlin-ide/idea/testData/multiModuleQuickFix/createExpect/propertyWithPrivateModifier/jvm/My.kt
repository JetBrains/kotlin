// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: "The declaration has `private` modifier"
// DISABLE-ERRORS

private actual val <caret>s: String = "s"