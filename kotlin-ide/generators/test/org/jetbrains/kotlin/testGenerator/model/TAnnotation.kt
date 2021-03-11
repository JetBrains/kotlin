package org.jetbrains.kotlin.testGenerator.model

sealed class TAnnotationValue {
    companion object {
        fun from(value: Any): TAnnotationValue {
            return when (value) {
                is String -> StringValue(value)
                is Class<*> -> ClassValue(value)
                else -> error("Unexpected annotation value: $value")
            }
        }
    }

    abstract fun render(): String

    class StringValue(private val value: String): TAnnotationValue() {
        override fun render() = '"' + value + '"'
    }

    class ClassValue(private val value: Class<*>): TAnnotationValue() {
        override fun render() = value.simpleName + ".class"
    }
}

class TAnnotation(className: String, val args: List<TAnnotationValue>) {
    val simpleName = className.substringAfterLast('.')
}

@Suppress("TestFunctionName")
inline fun <reified T : Annotation> TAnnotation(vararg args: Any): TAnnotation {
    return TAnnotation(T::class.java.name, args.map { TAnnotationValue.from(it) })
}