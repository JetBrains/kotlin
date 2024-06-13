// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

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

fun `should not work with local val for different type`() {
    val property = StringProperty("OK")
    <!VAL_REASSIGNMENT!>property<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
}

fun `should not work with local val for same type`() {
    val property = StringProperty("OK")
    <!VAL_REASSIGNMENT!>property<!> = StringProperty("Fail")
}

fun `should not work with local var for different type`() {
    var property = StringProperty("OK")
    property = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
}

fun `should work with local var for same type`() {
    var property = StringProperty("OK")
    property = StringProperty("Fail")
}

fun `should not work with method parameters`() {
    fun m1(property: StringProperty): Unit {
        <!VAL_REASSIGNMENT!>property<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
    }

    fun m2(property: StringProperty): Unit {
        <!VAL_REASSIGNMENT!>property<!> = StringProperty("Fail")
    }
}
