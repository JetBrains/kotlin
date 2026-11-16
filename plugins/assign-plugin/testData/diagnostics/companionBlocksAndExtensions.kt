// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
annotation class ValueContainer

@ValueContainer
data class StringProperty(var v: String) {
    companion {
        fun assign(v: String) {}
    }
    fun get(): String = v
}

companion fun StringProperty.assign(x: Int) {}

data class Task(val input: StringProperty)

fun `should report error if type doesn't match`() {
    val task = Task(StringProperty("Fail"))
    <!UNRESOLVED_REFERENCE!>task.input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> ""
    <!UNRESOLVED_REFERENCE!>task.input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
