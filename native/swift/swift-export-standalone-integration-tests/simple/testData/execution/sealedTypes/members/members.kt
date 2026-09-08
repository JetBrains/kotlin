// KIND: STANDALONE
// MODULE: SealedMembers
// FILE: main.kt

// Sealed types follow the ordinary override rules, so members declared on the root stay reachable
// both through the root and through the enum payload - including an `enum class` valued property
// and an override that lives in an indirect subclass.

enum class Status {
    ACTIVE, CLOSED
}

sealed class Account {
    abstract val id: String

    open val status: Status
        get() = Status.ACTIVE

    open fun label(): String = "account:$id"
}

class Personal(override val id: String) : Account() {
    override val status: Status = Status.CLOSED

    override fun label(): String = "personal:$id"
}

open class Business(override val id: String) : Account() {
    override fun label(): String = "business:$id"
}

class Subsidiary(id: String) : Business(id) {
    override val status: Status = Status.CLOSED

    override fun label(): String = "subsidiary:$id"
}

fun createPersonal(): Account = Personal("p-1")

fun createBusiness(): Account = Business("b-1")

fun createSubsidiary(): Account = Subsidiary("s-1")
