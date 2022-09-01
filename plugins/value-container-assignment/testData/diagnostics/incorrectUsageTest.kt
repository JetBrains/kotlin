// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE, -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// !RENDER_DIAGNOSTICS_FULL_TEXT

annotation class ValueContainer

@ValueContainer
data class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun assign(v: StringProperty) {
        this.v = v.get()
    }
    fun get(): String = v
}

data class Task(val input: StringProperty)

fun test() {
    run {
        // Should report error if type doesn't match
        val task = Task(StringProperty("Fail"))
        <!VAL_REASSIGNMENT!>task.input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }

    run {
        // Should report error if type doesn't match with apply
        val task = Task(StringProperty("Fail"))
        task.apply {
            <!VAL_REASSIGNMENT!>input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
        }
    }

    run {
        // obj[i] = v should not be translated to obj.get(i).assign(v)
        operator fun Task.get(i: Int) = this.input
        val task = Task(StringProperty("Fail"))
        task<!NO_SET_METHOD!><!UNRESOLVED_REFERENCE!>[<!>0<!UNRESOLVED_REFERENCE!>]<!><!> = StringProperty("Fail")
    }
}

