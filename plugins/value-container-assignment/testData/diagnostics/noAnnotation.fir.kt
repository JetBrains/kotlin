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

fun `should not work with assignment when there is no annotation on a type`() {
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
