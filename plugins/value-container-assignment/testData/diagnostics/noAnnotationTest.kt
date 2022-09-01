// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE, -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// !RENDER_DIAGNOSTICS_FULL_TEXT

data class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun assign(v: StringProperty) {
        this.v = v.get()
    }
    fun get(): String = v
}

fun StringProperty.assign(v: Int) = this.assign("OK")

data class Task(val input: StringProperty)

fun test() {
    // Should not work with assignment
    val task = Task(StringProperty("Fail"))
    <!VAL_REASSIGNMENT!>task.input<!> = <!TYPE_MISMATCH!>"OK"<!>
    <!VAL_REASSIGNMENT!>task.input<!> = StringProperty("OK")
    task.apply {
        <!VAL_REASSIGNMENT!>input<!> = <!TYPE_MISMATCH!>"OK"<!>
    }
    task.apply {
        <!VAL_REASSIGNMENT!>input<!> = StringProperty("OK")
    }
    <!VAL_REASSIGNMENT!>task.input<!> = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    task.apply {
        <!VAL_REASSIGNMENT!>input<!> = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }
}
