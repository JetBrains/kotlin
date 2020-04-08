// "Create expected property in common module testModule_Common" "true"
// DISABLE-ERRORS

actual var <caret>foo: String
    get() = field * field
    set(value) { field = value }