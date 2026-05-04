// LANGUAGE: +CompanionBlocksAndExtensions
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
    task.<!UNRESOLVED_REFERENCE!>input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> ""
    task.<!UNRESOLVED_REFERENCE!>input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> 1
}

