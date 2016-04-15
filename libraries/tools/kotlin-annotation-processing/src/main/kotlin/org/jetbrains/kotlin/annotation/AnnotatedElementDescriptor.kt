package org.jetbrains.kotlin.annotation

sealed class AnnotatedElementDescriptor(val classFqName: String) {
    class Class(classFqName: String) : AnnotatedElementDescriptor(classFqName) {
        // use referential equality
    }

    class Method(classFqName: String, val methodName: String) : AnnotatedElementDescriptor(classFqName) {
        override fun equals(other: Any?) = other is Method && methodName == other.methodName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + methodName.hashCode()
    }

    class Constructor(classFqName: String) : AnnotatedElementDescriptor(classFqName) {
        // use referential equality
    }

    class Field(classFqName: String, val fieldName: String) : AnnotatedElementDescriptor(classFqName) {
        override fun equals(other: Any?) = other is Field && fieldName == other.fieldName && classFqName == other.classFqName

        override fun hashCode() = 31 * classFqName.hashCode() + fieldName.hashCode()
    }
}