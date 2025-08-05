package java.lang

interface AutoCloseable {
    fun close()
}

open class ClassCastException : Throwable()
open class RuntimeException : Throwable()
open class AssertionError : Throwable()
open class Error : Throwable()

typealias Exception = kotlin.Exception
typealias String = kotlin.String
typealias Number = kotlin.Number
typealias Comparable<T> = kotlin.Comparable<T>
typealias Class<T> = kotlin.reflect.KClass<*>
typealias Void = Unit

object Long {
    fun valueOf(value: kotlin.Long): kotlin.Long = value
}

object Byte {
    fun valueOf(value: kotlin.Byte): kotlin.Byte = value
}

object System {
    fun getProperty(key: String, defaultValue: String? = null): String? =
        defaultValue
}