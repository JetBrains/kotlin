// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: identity_and_casts.kt

interface ParentRequirement {
    fun parentRequirement(): String
}

interface ChildRequirement : ParentRequirement {
    fun childRequirement(): String
}

open class CompleteRequirementBase : ChildRequirement {
    override fun parentRequirement(): String = "kotlin-parent"
    override fun childRequirement(): String = "kotlin-child"
}

class RequirementStorage {
    private var stored: CompleteRequirementBase? = null

    fun store(value: CompleteRequirementBase) {
        stored = value
    }

    fun retrieve(): CompleteRequirementBase? = stored
}

fun callParentRequirement(value: ParentRequirement): String = value.parentRequirement()
fun callChildRequirement(value: ChildRequirement): String = value.childRequirement()

interface CastParent {
    fun parentToken(): String
}

interface CastChild : CastParent {
    fun childToken(): String
}

open class CastBase : CastChild {
    override open fun parentToken(): String = "kotlin-parent-token"
    override open fun childToken(): String = "kotlin-child-token"
}

class CastStorage {
    private var stored: CastParent? = null

    fun store(value: CastParent) {
        stored = value
    }

    fun retrieve(): CastParent? = stored
}

fun echoCastParent(value: CastParent): CastParent = value
fun callCastParent(value: CastParent): String = value.parentToken()
fun callCastChild(value: CastChild): String = value.childToken()

class FieldPayload(val label: String)

open class FieldOwnerBase(initial: FieldPayload) {
    open var current: FieldPayload = initial
    open fun selected(): FieldPayload = current

    open fun replace(next: FieldPayload): FieldPayload {
        val previous = current
        current = next
        return previous
    }
}

fun readCurrentField(value: FieldOwnerBase): FieldPayload = value.current
fun writeCurrentField(value: FieldOwnerBase, payload: FieldPayload) {
    value.current = payload
}

fun callSelectedField(value: FieldOwnerBase): FieldPayload = value.selected()
fun replaceCurrentField(value: FieldOwnerBase, payload: FieldPayload): FieldPayload = value.replace(payload)
