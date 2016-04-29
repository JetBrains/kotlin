package org.jetbrains.kotlin.annotation

sealed class AnnotatedElement(val classFqName: String) {
    class Class(classFqName: String) : AnnotatedElement(classFqName) {
        override fun equals(other: Any?) = other is Class && classFqName == other.classFqName

        override fun hashCode() = classFqName.hashCode()
    }

    class Method(classFqName: String, val methodName: String) : AnnotatedElement(classFqName) {
        override fun equals(other: Any?) = other is Method && methodName == other.methodName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + methodName.hashCode()
    }

    class Constructor(classFqName: String) : AnnotatedElement(classFqName) {
        companion object {
            const val METHOD_NAME = "<init>"
        }

        override fun equals(other: Any?) = other is Constructor && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + METHOD_NAME.hashCode()
    }

    class Field(classFqName: String, val fieldName: String) : AnnotatedElement(classFqName) {
        override fun equals(other: Any?) = other is Field && fieldName == other.fieldName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + fieldName.hashCode()
    }
}

fun AnnotationWriter.writeAnnotatedElement(annotation: String, element: AnnotatedElement) {
    when (element) {
        is AnnotatedElement.Class -> {
            writeAnnotatedClass(annotation, element.classFqName)
        }
        is AnnotatedElement.Constructor -> {
            writeAnnotatedMethod(annotation, element.classFqName, AnnotatedElement.Constructor.METHOD_NAME)
        }
        is AnnotatedElement.Method -> {
            writeAnnotatedMethod(annotation, element.classFqName, element.methodName)
        }
        is AnnotatedElement.Field -> {
            writeAnnotatedField(annotation, element.classFqName, element.fieldName)
        }
    }
}