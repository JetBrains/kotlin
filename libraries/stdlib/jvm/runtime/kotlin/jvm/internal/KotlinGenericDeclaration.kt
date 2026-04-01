/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Method
import kotlin.reflect.KDeclarationContainer

/**
 * Common superinterface for Kotlin reflection objects which can have type parameters, and which provide a method [findJavaDeclaration]
 * to find the corresponding Java reflection object.
 */
public interface KotlinGenericDeclaration {
    /**
     * Returns a Java reflection class, method, constructor or field, corresponding to this Kotlin declaration,
     * or `null` if the Java reflection object could not be found for some reason.
     */
    public fun findJavaDeclaration(): GenericDeclaration?
}

public fun KDeclarationContainer?.findMethodBySignature(signature: String): GenericDeclaration? {
    if (this !is ClassBasedDeclarationContainer) return null
    val jvmName = signature.substringBefore('(')
    if (jvmName == "<init>") {
        throw UnsupportedOperationException("Generic Java constructors are not supported: $this/$signature")
    }
    return jClass.declaredMethods.find {
        it.name == jvmName && it.computeMethodSignature() == signature
    }
}

private fun Method.computeMethodSignature(): String =
    buildString {
        append(name)
        append("(")
        for (type in parameterTypes) {
            appendClass(type)
        }
        append(")")
        appendClass(returnType)
    }

private fun Appendable.appendClass(start: Class<*>) {
    var klass = start
    while (klass.isArray) {
        append("[")
        klass = klass.componentType
    }
    when (klass) {
        Void.TYPE -> append("V")
        Int::class.javaPrimitiveType -> append("I")
        Long::class.javaPrimitiveType -> append("J")
        Short::class.javaPrimitiveType -> append("S")
        Byte::class.javaPrimitiveType -> append("B")
        Boolean::class.javaPrimitiveType -> append("Z")
        Char::class.javaPrimitiveType -> append("C")
        Float::class.javaPrimitiveType -> append("F")
        Double::class.javaPrimitiveType -> append("D")
        else -> {
            append("L")
            append(klass.name.replace('.', '/'))
            append(";")
        }
    }
}
