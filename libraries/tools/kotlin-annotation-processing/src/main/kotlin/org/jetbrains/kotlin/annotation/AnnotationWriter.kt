package org.jetbrains.kotlin.annotation

import java.io.Writer
import org.jetbrains.kotlin.annotation.CompactNotationType as Notation

abstract class AnnotationWriter(private val writer: Writer) {
    companion object {
        private val LINE_SEP = System.getProperty("line.separator")
    }

    abstract fun writeClassDeclaration(classFqName: String)
    abstract fun writeAnnotatedClass(annotationName: String, classFqName: String)
    abstract fun writeAnnotatedMethod(annotationName: String, classFqName: String, methodName: String)
    abstract fun writeAnnotatedField(annotationName: String, classFqName: String, fieldName: String)

    protected fun writeLine(vararg parts: String?) {
        var i = 0
        for (part in parts) {
            if (part == null) continue
            if (i > 0) {
                writer.write(" ")
            }
            writer.write(part)
            i++
        }
        writer.write(LINE_SEP)
    }
}
