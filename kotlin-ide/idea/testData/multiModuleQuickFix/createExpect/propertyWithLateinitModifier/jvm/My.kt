// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: "The declaration has `lateinit` modifier"
// DISABLE-ERRORS

actual lateinit var <caret>s: String