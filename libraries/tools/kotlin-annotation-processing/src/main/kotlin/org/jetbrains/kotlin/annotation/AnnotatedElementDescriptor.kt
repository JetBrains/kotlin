package org.jetbrains.kotlin.annotation

sealed class AnnotatedElementDescriptor(val classFqName: String) {
    class Class(classFqName: String) : AnnotatedElementDescriptor(classFqName) {
        override fun equals(other: Any?) = other is Class && classFqName == other.classFqName

        override fun hashCode() = classFqName.hashCode()
    }

    class Method(classFqName: String, val methodName: String) : AnnotatedElementDescriptor(classFqName) {
        override fun equals(other: Any?) = other is Method && methodName == other.methodName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + methodName.hashCode()
    }

    class Constructor(classFqName: String) : AnnotatedElementDescriptor(classFqName) {
        companion object {
            const val METHOD_NAME = "<init>"
        }

        override fun equals(other: Any?) = other is Constructor && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + METHOD_NAME.hashCode()
    }

    class Field(classFqName: String, val fieldName: String) : AnnotatedElementDescriptor(classFqName) {
        override fun equals(other: Any?) = other is Field && fieldName == other.fieldName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + fieldName.hashCode()
    }
}

fun AnnotationWriter.writeAnnotatedElement(annotation: String, element: AnnotatedElementDescriptor) {
    when (element) {
        is AnnotatedElementDescriptor.Class -> {
            writeAnnotatedClass(annotation, element.classFqName)
        }
        is AnnotatedElementDescriptor.Constructor -> {
            writeAnnotatedMethod(annotation, element.classFqName, AnnotatedElementDescriptor.Constructor.METHOD_NAME)
        }
        is AnnotatedElementDescriptor.Method -> {
            writeAnnotatedMethod(annotation, element.classFqName, element.methodName)
        }
        is AnnotatedElementDescriptor.Field -> {
            writeAnnotatedField(annotation, element.classFqName, element.fieldName)
        }
    }
}