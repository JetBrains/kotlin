/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class JavaLangObjectToString(context: DefaultExecutionContext) : BaseMirror<ObjectReference, String>("java.lang.Object", context) {
    private val toString by MethodDelegate<StringReference>("toString", "()Ljava/lang/String;")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): String {
        return toString.value(value, context)?.value() ?: ""
    }
}

class JavaUtilAbstractCollection(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfJavaLangAbstractCollection>("java.util.AbstractCollection", context) {
    private val abstractList = JavaUtilAbstractList(context)
    private val sizeMethod by MethodDelegate<IntegerValue>("size")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfJavaLangAbstractCollection? {
        val list = mutableListOf<ObjectReference>()
        val size = sizeMethod.value(value, context)?.intValue() ?: 0
        for (index in 0 until size) {
            val reference = abstractList.get(value, index, context) ?: continue
            list.add(reference)
        }
        return MirrorOfJavaLangAbstractCollection(value, list)
    }
}

class JavaUtilAbstractList(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, ObjectReference>("java.util.AbstractList", context) {
    val getMethod by MethodDelegate<ObjectReference>("get")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): Nothing? =
            null

    fun get(value: ObjectReference, index: Int, context: DefaultExecutionContext): ObjectReference? =
            getMethod.value(value, context, context.vm.mirrorOf(index))
}

class WeakReference constructor(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfWeakReference>("java.lang.ref.WeakReference", context) {
    val get by MethodDelegate<ObjectReference>("get")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfWeakReference? {
        return MirrorOfWeakReference(value, get.value(value, context))
    }
}

class StackTraceElement(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfStackTraceElement>("java.lang.StackTraceElement", context) {
    private val declaringClassObjectField by FieldDelegate<ObjectReference>("declaringClass")
    private val moduleNameField by FieldDelegate<StringReference>("moduleName")
    private val moduleVersionField by FieldDelegate<StringReference>("moduleVersion")
    private val declaringClassField by FieldDelegate<StringReference>("declaringClass")
    private val methodNameField by FieldDelegate<StringReference>("methodName")
    private val fileNameField by FieldDelegate<StringReference>("fileName")
    private val lineNumberField by FieldDelegate<IntegerValue>("lineNumber")
    private val formatField by FieldDelegate<ByteValue>("format")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStackTraceElement? {
        val declaringClassObject = declaringClassObjectField.value(value)
        val moduleName = moduleNameField.value(value)?.value()
        val moduleVersion = moduleVersionField.value(value)?.value()
        val declaringClass = declaringClassField.value(value)?.value()
        val methodName = methodNameField.value(value)?.value()
        val fileName = fileNameField.value(value)?.value()
        val lineNumber = lineNumberField.value(value)?.value()
        val format = formatField.value(value)?.value()
        return MirrorOfStackTraceElement(
                value,
                declaringClassObject,
                moduleName,
                moduleVersion,
                declaringClass,
                methodName,
                fileName,
                lineNumber,
                format
        )
    }
}


