// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// KT-30616

val foo = "hello"

<!SCRIPT_CAPTURING_ENUM!>enum class Bar(val s: String) {
    Eleven(s = foo)
}<!>

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration, stringLiteral */
