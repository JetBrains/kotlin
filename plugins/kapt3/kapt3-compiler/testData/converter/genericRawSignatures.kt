// IGNORE_BACKEND: JVM_IR

class GenericRawSignatures {
    fun <T> genericFun(): T? = null
    fun nonGenericFun(): String? = null
}
