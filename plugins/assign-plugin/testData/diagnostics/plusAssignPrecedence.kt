annotation class ValueContainer

abstract class AbstractStringProperty(protected var v: String) {
    fun get(): String {
        return v
    }
}

@ValueContainer
class StringProperty(v: String) : AbstractStringProperty(v) {
    fun assign(v: String) {
        this.v = v
    }

    fun assign(v: StringProperty) {
        this.v = v.get()
    }
}

@ValueContainer
class StringPropertyWithPlus(v: String) : AbstractStringProperty(v) {
    fun assign(v: String) {
        this.v = v
    }

    fun assign(o: StringPropertyWithPlus) {
        this.v = o.get()
    }

    operator fun plus(v: String) =
        StringPropertyWithPlus(this.v + v)
}

@ValueContainer
class StringPropertyWithPlusAssign(v: String) : AbstractStringProperty(v) {
    fun assign(v: String) {
        this.v = v
    }

    fun assign(o: StringPropertyWithPlusAssign) {
        this.v = o.get()
    }

    operator fun plusAssign(v: String) {
        this.v += v
    }
}

@ValueContainer
class StringPropertyWithPlusAndPlusAssign(v: String) : AbstractStringProperty(v) {
    fun assign(v: String) {
        this.v = v
    }

    fun assign(o: StringPropertyWithPlusAndPlusAssign) {
        this.v = o.get()
    }

    operator fun plus(v: String) =
        StringPropertyWithPlusAndPlusAssign(this.v + v)

    operator fun plusAssign(v: String) {
        this.v += v
    }
}

data class Task(
    val valInput: StringProperty,
    var varInput: StringProperty,

    val valInputWithPlus: StringPropertyWithPlus,
    var varInputWithPlus: StringPropertyWithPlus,

    val valInputWithPlusAssign: StringPropertyWithPlusAssign,
    var varInputWithPlusAssign: StringPropertyWithPlusAssign,

    val valInputWithPlusAndPlusAssign: StringPropertyWithPlusAndPlusAssign,
    var varInputWithPlusAndPlusAssign: StringPropertyWithPlusAndPlusAssign,
)

fun box(): String {
    val task = Task(
        StringProperty("O"),
        StringProperty("O"),

        StringPropertyWithPlus("O"),
        StringPropertyWithPlus("O"),

        StringPropertyWithPlusAssign("O"),
        StringPropertyWithPlusAssign("O"),

        StringPropertyWithPlusAndPlusAssign("O"),
        StringPropertyWithPlusAndPlusAssign("O")
    )

    task.valInput <!UNRESOLVED_REFERENCE!>+=<!> "K"
    task.varInput <!UNRESOLVED_REFERENCE!>+=<!> "K"

    task.valInputWithPlus += "K"

    task.varInputWithPlusAndPlusAssign <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> "K"

    return "OK"
}
