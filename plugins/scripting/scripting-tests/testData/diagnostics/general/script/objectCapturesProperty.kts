// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT


fun foo() = B.bar()

val life = 42

<!SCRIPT_CAPTURING_OBJECT!>object B<!> {
    fun bar() = life
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, objectDeclaration, propertyDeclaration */
