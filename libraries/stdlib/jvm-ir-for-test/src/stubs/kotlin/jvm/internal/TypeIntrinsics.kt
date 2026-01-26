package kotlin.jvm.internal

import kotlin.Function

object TypeIntrinsics {
    @JvmStatic
    fun isFunctionOfArity(obj: Any?, arity: Int): Boolean {
        return obj is Function<*> && (obj as FunctionBase<*>).arity == arity
    }

    @JvmStatic
    fun throwCce(obj: Any?, message: String) {
        throw ClassCastException(message)
    }
    
    @JvmStatic
    fun throwCce(message: String) {
        throw ClassCastException(message)
    }
}
