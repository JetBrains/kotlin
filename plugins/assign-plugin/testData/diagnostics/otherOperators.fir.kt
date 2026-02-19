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

fun `should not effect error reporting for other operators`() {
    val task = Task(StringProperty("Fail"))
    val nullTask: Task? = null

    // a.b += c
    task.input <!UNRESOLVED_REFERENCE!>+=<!> StringProperty("Fail")
    nullTask?.input <!UNRESOLVED_REFERENCE!>+=<!> StringProperty("Fail")

    // a.b <= c
    task.input <!UNRESOLVED_REFERENCE!><=<!> StringProperty("Fail")
    nullTask?.input <!UNRESOLVED_REFERENCE!><=<!> StringProperty("Fail")

    // a.b >= c
    task.input <!UNRESOLVED_REFERENCE!>>=<!> StringProperty("Fail")
    nullTask?.input <!UNRESOLVED_REFERENCE!>>=<!> StringProperty("Fail")

    // a.b[c] = d
    task.input<!NO_SET_METHOD!>[0]<!> = StringProperty("Fail")
    nullTask?.input<!NO_SET_METHOD!>[0]<!> = StringProperty("Fail")

    // a.b[c, d] = e
    task.input<!NO_SET_METHOD!>[0, 0]<!> = StringProperty("Fail")
    nullTask?.input<!NO_SET_METHOD!>[0, 0]<!> = StringProperty("Fail")

    // a.b[c,..,d] = e
    task.input<!NO_SET_METHOD!>[0, 0, 0]<!> = StringProperty("Fail")
    nullTask?.input<!NO_SET_METHOD!>[0, 0, 0]<!> = StringProperty("Fail")

    // a?.b[c] += d
    <!OPERATOR_MODIFIER_REQUIRED!>task.input[<!TOO_MANY_ARGUMENTS!>0<!>]<!> <!UNRESOLVED_REFERENCE!>+=<!> StringProperty("Fail")
    <!OPERATOR_MODIFIER_REQUIRED!>nullTask?.input[<!TOO_MANY_ARGUMENTS!>0<!>]<!> <!UNRESOLVED_REFERENCE!>+=<!> StringProperty("Fail")

    // a[i] = b should not be translated to a.get(i).assign(b)
    operator fun Task.get(i: Int) = this.input
    task<!NO_SET_METHOD!>[0]<!> = StringProperty("Fail")

    // a.get(i) = b should not be translated to a.get(i).assign(b)
    task.<!VARIABLE_EXPECTED!>get(0)<!> = StringProperty("Fail")
    nullTask?.<!VARIABLE_EXPECTED!>get(0)<!> = StringProperty("Fail")
}
