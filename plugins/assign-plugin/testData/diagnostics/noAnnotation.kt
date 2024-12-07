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
