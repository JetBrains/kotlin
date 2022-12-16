annotation class ValueContainer

@ValueContainer
data class StringProperty(var v: String) {
    fun assign(v: String): <!DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT!>String<!> {
        this.v = v
        return ""
    }
    fun assign(v: StringProperty): <!DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT!>String<!> {
        this.v = v.get()
        return ""
    }
    fun get(): String = v
}

fun StringProperty.assign(v: Int): <!DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT!>String<!> {
    this.v = "OK"
    return ""
}

data class Task(val input: StringProperty)

fun `should report an error for assign method return type on assignment for annotated class`() {
    val task = Task(StringProperty("Fail"))
    task.input <!CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT!>=<!> "42"
    task.input <!CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT!>=<!> 42
}

fun `should not report an error for assign return type for unannotated class`() {
    data class IntProperty(var v: Int)
    fun IntProperty.assign(v: Int): String = "OK"
    data class IntTask(val input: IntProperty)

    val task = IntTask(IntProperty(42))
    task.<!VAL_REASSIGNMENT!>input<!> = <!ASSIGNMENT_TYPE_MISMATCH!>42<!>
}
