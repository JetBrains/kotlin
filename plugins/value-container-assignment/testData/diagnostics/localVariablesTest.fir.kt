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

fun test() {
    run {
        // Should not work with local val for different type
        val property = StringProperty("OK")
        <!VAL_REASSIGNMENT!>property<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
    }

    run {
        // Should not work with local val for same type
        val property = StringProperty("OK")
        <!VAL_REASSIGNMENT!>property<!> = StringProperty("Fail")
    }

    run {
        // Should not work with local var for different type
        var property = StringProperty("OK")
        property = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
    }

    run {
        // Should work with local var for same type
        var property = StringProperty("OK")
        property = StringProperty("Fail")
    }

    run {
        // Should not work with method parameters
        fun m1(property: StringProperty): Unit {
            <!VAL_REASSIGNMENT!>property<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
        }
        fun m2(property: StringProperty): Unit {
            <!VAL_REASSIGNMENT!>property<!> = StringProperty("Fail")
        }
    }
}
