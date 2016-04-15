package org.jetbrains.kotlin.annotation

import java.io.Writer
import org.jetbrains.kotlin.annotation.CompactNotationType as Notation

class CompactAnnotationWriter(writer: Writer) : AnnotationWriter(writer) {
    private val annotationsShortener = NameShortener(Notation.SHORTENED_ANNOTATION)
    private val packageShortener = NameShortener(Notation.SHORTENED_PACKAGE_NAME)

    override fun writeClassDeclaration(classFqName: String) {
        writeLine(Notation.CLASS_DECLARATION, getShortenedClassName(classFqName))
    }

    override fun writeAnnotatedClass(annotationName: String, classFqName: String) {
        writeAnnotated(Notation.ANNOTATED_CLASS, annotationName, classFqName)
    }

    override fun writeAnnotatedMethod(annotationName: String, classFqName: String, methodName: String) {
        writeAnnotated(Notation.ANNOTATED_METHOD, annotationName, classFqName, methodName)
    }

    override fun writeAnnotatedField(annotationName: String, classFqName: String, fieldName: String) {
        writeAnnotated(Notation.ANNOTATED_FIELD, annotationName, classFqName, fieldName)
    }

    private fun writeAnnotated(type: String, annotationName: String, className: String, memberName: String? = null) {
        writeLine(type, annotationsShortener[annotationName].toString(), getShortenedClassName(className), memberName)
    }

    private fun getShortenedClassName(fqName: String): String {
        val lastDotIndex = fqName.lastIndexOf('.')
        if (lastDotIndex == -1) return fqName

        val packageName = fqName.substring(0, lastDotIndex)
        val simpleName = fqName.substring(lastDotIndex + 1)

        return "${packageShortener[packageName]}/$simpleName"
    }

    private inner class NameShortener(private val type: String) {
        private val names = hashMapOf<String, Int>()

        operator fun get(name: String): Int =
                names.getOrPut(name) {
                    val id = names.size
                    writeLine(type, name, "$id")
                    id
                }
    }
}
