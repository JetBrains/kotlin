package kotlin.reflect.js.internal

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal abstract class KFunctionImpl(val flags: Int, val arity: Int, val id: String) {
    protected open fun computeReceiver(): Any? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KFunctionImpl &&
                this.flags == other.flags &&
                this.arity == other.arity &&
                this.id == other.id &&
                this.computeReceiver() == other.computeReceiver()
    }

    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + arity
        result = 31 * result + id.hashCode()
        result = 31 * result + computeReceiver().hashCode()
        return result
    }
}

@UsedFromCompilerGeneratedCode
internal fun createComparableReference(func: Any): dynamic = func.apply {
    JsObject.setPrototypeOf(this, KFunctionImpl::class.js.asDynamic().prototype)
}