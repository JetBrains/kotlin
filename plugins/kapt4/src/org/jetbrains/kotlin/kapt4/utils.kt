/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.org.objectweb.asm.Opcodes

internal val PsiModifierListOwner.isPublic: Boolean get() = hasModifier(JvmModifier.PUBLIC)
internal val PsiModifierListOwner.isPrivate: Boolean get() = hasModifier(JvmModifier.PRIVATE)
internal val PsiModifierListOwner.isProtected: Boolean get() = hasModifier(JvmModifier.PROTECTED)

internal val PsiModifierListOwner.isFinal: Boolean get() = hasModifier(JvmModifier.FINAL)
internal val PsiModifierListOwner.isAbstract: Boolean get() = hasModifier(JvmModifier.ABSTRACT)

internal val PsiModifierListOwner.isStatic: Boolean get() = hasModifier(JvmModifier.STATIC)
internal val PsiModifierListOwner.isVolatile: Boolean get() = hasModifier(JvmModifier.VOLATILE)
internal val PsiModifierListOwner.isSynchronized: Boolean get() = hasModifier(JvmModifier.SYNCHRONIZED)
internal val PsiModifierListOwner.isNative: Boolean get() = hasModifier(JvmModifier.NATIVE)
internal val PsiModifierListOwner.isStrict: Boolean get() = hasModifier(JvmModifier.STRICTFP)
internal val PsiModifierListOwner.isTransient: Boolean get() = hasModifier(JvmModifier.TRANSIENT)

internal typealias JavacList<T> = com.sun.tools.javac.util.List<T>

internal inline fun <T, R> mapJList(values: Array<T>?, f: (T) -> R?): JavacList<R> {
    return mapJList(values?.asList(), f)
}

internal inline fun <T, R> mapJList(values: Iterable<T>?, f: (T) -> R?): JavacList<R> {
    if (values == null) return JavacList.nil()

    var result = JavacList.nil<R>()
    for (item in values) {
        f(item)?.let { result = result.append(it) }
    }
    return result
}

internal inline fun <T, R> mapJListIndexed(values: Iterable<T>?, f: (Int, T) -> R?): JavacList<R> {
    if (values == null) return JavacList.nil()

    var result = JavacList.nil<R>()
    values.forEachIndexed { index, item ->
        f(index, item)?.let { result = result.append(it) }
    }
    return result
}

internal operator fun <T : Any> JavacList<T>.plus(other: JavacList<T>): JavacList<T> {
    return this.appendList(other)
}

internal val PsiMethod.signature: String
    get() = ClassUtil.getAsmMethodSignature(this)

internal val PsiField.signature: String
    get() = getAsmFieldSignature(this)

private fun getAsmFieldSignature(field: PsiField): String {
    return ClassUtil.getBinaryPresentation(field.type)
}

internal val PsiType.qualifiedName: String
    get() = qualifiedNameOrNull ?: canonicalText.replace("""<.*>""".toRegex(), "")

internal val PsiType.qualifiedNameOrNull: String?
    get() {
        if (this is PsiPrimitiveType) return name
        if (this is PsiWildcardType) return this.bound?.qualifiedNameOrNull
        return when (val resolvedClass = resolvedClass) {
            is PsiTypeParameter -> resolvedClass.name
            else -> resolvedClass?.qualifiedName
        }
    }

internal val PsiType.simpleNameOrNull: String?
    get() {
        if (this is PsiPrimitiveType) return name
        return when (val resolvedClass = resolvedClass) {
            is PsiTypeParameter -> resolvedClass.name
            else -> resolvedClass?.name
        }
    }

internal val PsiClass.defaultType: PsiType
    get() = PsiTypesUtil.getClassType(this)

internal val PsiType.resolvedClass: PsiClass?
    get() = (this as? PsiClassType)?.resolve()

internal val PsiModifierListOwner.accessFlags: Long
    get() = when (this) {
        is PsiClass -> computeClassAccessFlags(this)
        is PsiMethod -> computeMethodAccessFlags(this)
        is PsiField -> computeFieldAccessFlags(this)
        else -> 0
    }.toLong()

private fun computeCommonAccessFlags(declaration: PsiModifierListOwner): Int {
    /*
     * int ACC_STATIC = 0x0008; // field, method; class isn't mentioned but actually used
     * int ACC_PUBLIC = 0x0001; // class, field, method
     * int ACC_PRIVATE = 0x0002; // class, field, method
     * int ACC_PROTECTED = 0x0004; // class, field, method
     * int ACC_FINAL = 0x0010; // class, field, method, parameter
     * int ACC_DEPRECATED = 0x20000; // class, field, method
     */
    var access = 0
    val visibilityFlag = when {
        declaration.isPublic -> Opcodes.ACC_PUBLIC
        declaration.isPrivate -> Opcodes.ACC_PRIVATE
        declaration.isProtected -> Opcodes.ACC_PROTECTED
        else -> 0
    }
    access = access or visibilityFlag
    if (declaration.isFinal) {
        access = access or Opcodes.ACC_FINAL
    }
    if (declaration.annotations.any { it.hasQualifiedName(StandardNames.FqNames.deprecated.asString()) }) {
        access = access or Opcodes.ACC_DEPRECATED
    }
    if (declaration.isStatic) {
        access = access or Opcodes.ACC_STATIC
    }
    return access
}

private fun computeClassAccessFlags(klass: PsiClass): Int {
    /*
     * int ACC_INTERFACE = 0x0200; // class
     * int ACC_ABSTRACT = 0x0400; // class, method
     * int ACC_ANNOTATION = 0x2000; // class
     * int ACC_ENUM = 0x4000; // class(?) field inner
     * int ACC_RECORD = 0x10000; // class
     */
    var access = computeCommonAccessFlags(klass)
    val classKindFlag = when {
        klass.isInterface -> Opcodes.ACC_INTERFACE
        klass.isEnum -> {
            // enum can not be final
            access = access and Opcodes.ACC_FINAL.inv()
            Opcodes.ACC_ENUM
        }

        klass.isRecord -> Opcodes.ACC_RECORD
        else -> 0
    }
    access = access or classKindFlag
    if (klass.isAnnotationType) {
        access = access or Opcodes.ACC_ANNOTATION
    }
    if (klass.isAbstract) {
        access = access or Opcodes.ACC_ABSTRACT
    }
    return access
}

private fun computeMethodAccessFlags(method: PsiMethod): Int {
    /*
     * int ACC_SYNCHRONIZED = 0x0020; // method
     * int ACC_VARARGS = 0x0080; // method
     * int ACC_NATIVE = 0x0100; // method
     * int ACC_ABSTRACT = 0x0400; // class, method
     * int ACC_STRICT = 0x0800; // method
     */
    var access = computeCommonAccessFlags(method)

    if (method.isSynchronized) {
        access = access or Opcodes.ACC_SYNCHRONIZED
    }
    if (method.isVarArgs) {
        access = access or Opcodes.ACC_VARARGS
    }
    if (method.isNative) {
        access = access or Opcodes.ACC_NATIVE
    }
    if (method.isAbstract) {
        access = access or Opcodes.ACC_ABSTRACT
    }
    if (method.isStrict) {
        access = access or Opcodes.ACC_STRICT
    }
    return access
}

private fun computeFieldAccessFlags(field: PsiField): Int {
    /*
     * int ACC_VOLATILE = 0x0040; // field
     * int ACC_TRANSIENT = 0x0080; // field
     * int ACC_ENUM = 0x4000; // class(?) field inner
     */
    var access = computeCommonAccessFlags(field)
    if (field.isVolatile) {
        access = access or Opcodes.ACC_VOLATILE
    }
    if (field.isTransient) {
        access = access or Opcodes.ACC_TRANSIENT
    }
    if (field is PsiEnumConstant) {
        access = access or Opcodes.ACC_ENUM
    }
    return access
}

internal val PsiClass.qualifiedNameWithDollars: String?
    get() {
        val packageName = PsiUtil.getPackageName(this) ?: return null
        if (packageName.isBlank()) {
            return qualifiedName?.replace(".", "$") ?: return null
        }

        val qualifiedName = this.qualifiedName ?: return null
        val className = qualifiedName.substringAfter("$packageName.")
        val classNameWithDollars = className.replace(".", "$")
        return "$packageName.$classNameWithDollars"
    }

private const val LONG_DEPRECATED = Opcodes.ACC_DEPRECATED.toLong()
private const val LONG_ENUM = Opcodes.ACC_ENUM.toLong()

internal fun isDeprecated(access: Long) = (access and LONG_DEPRECATED) != 0L
internal fun isEnum(access: Long) = (access and LONG_ENUM) != 0L