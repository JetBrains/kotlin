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
    task.input = <!ASSIGNMENT_TYPE_MISMATCH!>"OK"<!>
    task.input = StringProperty("OK")
    task.apply {
        input = <!ASSIGNMENT_TYPE_MISMATCH!>"OK"<!>
    }
    task.apply {
        input = StringProperty("OK")
    }
    task.input = <!ASSIGNMENT_TYPE_MISMATCH!>42<!>
    task.apply {
        input = <!ASSIGNMENT_TYPE_MISMATCH!>42<!>
    }
}
