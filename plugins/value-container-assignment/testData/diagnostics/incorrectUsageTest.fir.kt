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
        <!ARGUMENT_TYPE_MISMATCH!>task.<!NONE_APPLICABLE!>input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> 42<!>
    }

    run {
        // Should report error if type doesn't match with apply
        val task = Task(StringProperty("Fail"))
        task.apply {
            <!NONE_APPLICABLE!>input<!> <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> 42
        }
    }

    run {
        // obj[i] = v should not be translated to obj.get(i).assign(v)
        operator fun Task.get(i: Int) = this.input
        val task = Task(StringProperty("Fail"))
        <!ARGUMENT_TYPE_MISMATCH!>task<!NO_SET_METHOD!>[0]<!> = StringProperty("Fail")<!>
    }
}
