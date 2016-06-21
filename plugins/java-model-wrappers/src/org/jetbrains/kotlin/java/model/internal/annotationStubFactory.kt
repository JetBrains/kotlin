/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.java.model.internal

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.KtLightAnnotation
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.net.URL
import java.net.URLClassLoader

internal fun createAnnotation(
        annotation: PsiAnnotation,
        annotationDeclaration: PsiClass,
        annotationClass: Class<*>
): Annotation? {
    val qualifiedName = annotationDeclaration.qualifiedName ?: return null
    val implQualifiedName = "stub." + qualifiedName + ".Impl" + Integer.toHexString(annotation.text.hashCode())
    
    val annotationInternalName = annotationDeclaration.getInternalName()
    val implInternalName = implQualifiedName.replace('.', '/')

    val bytes = createAnnotationImplementationClass(annotation, annotationDeclaration, annotationInternalName, implInternalName)
    return loadClass(implQualifiedName, bytes, annotationClass)?.newInstance() as? Annotation
}

private fun createAnnotationImplementationClass(
        annotation: PsiAnnotation, 
        annotationDeclaration: PsiClass, 
        annotationInternalName: String, 
        implInternalName: String
): ByteArray {
    return ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS).apply {
        visit(V1_6, ACC_SUPER or ACC_PUBLIC, implInternalName, null, "java/lang/Object", arrayOf(annotationInternalName))

        with(visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)) {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitInsn(RETURN)
            visitMaxs(-1, -1)
            visitEnd()
        }

        with(visitMethod(ACC_PUBLIC, "annotationType", "()Ljava/lang/Class;",
                         "()Ljava/lang/Class<+Ljava/lang/annotation/Annotation;>;", null)) {
            visitCode()
            visitLdcInsn(Type.getType("L$annotationInternalName;"))
            visitInsn(ARETURN)
            visitMaxs(-1, -1)
            visitEnd()
        }

        if (annotationDeclaration.allMethods.isNotEmpty()) {
            val evaluator = JavaPsiFacade.getInstance(annotation.project).constantEvaluationHelper

            for (method in annotationDeclaration.methods) {
                if (method !is PsiAnnotationMethod) continue

                if (method.returnType == null) continue
                val returnType = method.returnType!!

                val value = annotation.findAttributeValue(method.name) ?: method.defaultValue

                when (returnType) {
                    PsiType.VOID -> unexpectedType("void")
                    PsiType.NULL -> unexpectedType("null")
                    is PsiClassType -> {
                        val resolvedClass = returnType.resolve()
                        val resolvedQualifiedName = resolvedClass?.qualifiedName
                        if (resolvedQualifiedName == "java.lang.String") {
                            writeStringMethod(method, calculateConstantValue(value, evaluator))
                        }
                        else if (resolvedClass != null && resolvedClass.isEnum) {
                            writeEnumMethod(method, resolvedClass, value)
                        }
                        else {
                            unexpectedType("$resolvedQualifiedName")
                        }
                    }
                    is PsiPrimitiveType -> writeLiteralMethod(method, calculateConstantValue(value, evaluator))
                    is PsiArrayType -> writeArrayMethod(method, value, evaluator)
                    else -> unexpectedType(returnType)
                }
            }
        }

        visitEnd()
    }.toByteArray()
}

private fun ClassWriter.writeEnumMethod(method: PsiAnnotationMethod, enumClass: PsiClass, value: PsiAnnotationMemberValue?) {
    val enumInternalName = enumClass.getInternalName()
    val enumConstant = (value?.originalElement as? PsiReference)?.resolve() as? PsiEnumConstant ?: return

    val mv = visitMethod(ACC_PUBLIC, method.name, "()L$enumInternalName;", null, null)
    
    with (InstructionAdapter(mv)) {
        visitCode()
        
        getstatic(enumInternalName, enumConstant.name!!, "L$enumInternalName;")
        visitInsn(ARETURN)

        visitMaxs(-1 ,-1)
        visitEnd()
    }
}

private fun ClassWriter.writeStringMethod(method: PsiAnnotationMethod, value: Any?) {
    val stringValue = (value as? String) ?: null

    val mv = visitMethod(ACC_PUBLIC, method.name, "()Ljava/lang/String;", null, null)
    
    with (InstructionAdapter(mv)) {
        visitCode()
        
        if (stringValue != null) {
            visitLdcInsn(stringValue)
        } else {
            visitInsn(ACONST_NULL)
        }
        visitInsn(ARETURN)
        
        visitMaxs(-1 ,-1)
        visitEnd()
    }
}

private fun ClassWriter.writeArrayMethod(
        method: PsiAnnotationMethod, 
        value: PsiAnnotationMemberValue?,
        evaluator: PsiConstantEvaluationHelper
) {
    val componentType = (method.returnType as? PsiArrayType)?.componentType ?: return
    val componentAsmType = componentType.toAsmType() ?: return
    
    val initializers = (value as? PsiArrayInitializerMemberValue)?.initializers ?: emptyArray()

    val mv = visitMethod(ACC_PUBLIC, method.name, "()[" + componentAsmType.descriptor, null, null)
    
    with (InstructionAdapter(mv)) {
        visitCode()
        
        iconst(initializers.size)
        newarray(componentAsmType)
        
        initializers.forEachIndexed { index, value ->
            val valueObject = calculateConstantValue(value, evaluator)
            
            dup()
            iconst(index)
            putValue(componentAsmType, valueObject)
            astore(componentAsmType)
        }

        visitInsn(ARETURN)
        visitMaxs(-1, -1)
        visitEnd()
    }
}

private fun calculateConstantValue(value: PsiAnnotationMemberValue?, evaluator: PsiConstantEvaluationHelper) = when (value) {
    is PsiLiteral -> value.value
    is KtLightAnnotation.LightExpressionValue<*> -> value.getConstantValue()
    is PsiExpression -> evaluator.computeConstantExpression(value)
    else -> null
}

private fun InstructionAdapter.putValue(type: Type, value: Any?) = when (type) {
    Type.DOUBLE_TYPE -> dconst(doubleValue(value))
    Type.FLOAT_TYPE -> fconst(floatValue(value))
    Type.LONG_TYPE -> lconst(longValue(value))
    Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.CHAR_TYPE, Type.BOOLEAN_TYPE -> iconst(type.castValue(value))
    else -> aconst(value)
}

private fun ClassWriter.writeLiteralMethod(method: PsiAnnotationMethod, value: Any?) {
    val returnType = method.returnType ?: return
    val asmType = returnType.toAsmType() ?: return
    
    val mv = visitMethod(ACC_PUBLIC, method.name, "()" + asmType.descriptor, null, null)
    with (InstructionAdapter(mv)) {
        visitCode()

        putValue(asmType, value)
        areturn(asmType)
        
        visitMaxs(-1, -1)
        visitEnd()
    }
}

private fun PsiType.toAsmType(): Type? = when (this) {
    PsiType.BYTE -> Type.BYTE_TYPE
    PsiType.SHORT -> Type.SHORT_TYPE
    PsiType.INT -> Type.INT_TYPE
    PsiType.CHAR -> Type.CHAR_TYPE
    PsiType.BOOLEAN -> Type.BOOLEAN_TYPE
    PsiType.LONG -> Type.LONG_TYPE
    PsiType.FLOAT -> Type.FLOAT_TYPE
    PsiType.DOUBLE -> Type.DOUBLE_TYPE
    is PsiClassType -> resolve()?.let { Type.getObjectType(it.getInternalName()) }
    else -> null
}

private fun Type.castValue(value: Any?): Int = when (this) {
    Type.BYTE_TYPE -> byteValue(value)
    Type.SHORT_TYPE -> shortValue(value)
    Type.INT_TYPE -> intValue(value)
    Type.CHAR_TYPE -> charValue(value)
    Type.BOOLEAN_TYPE -> booleanValue(value)
    else -> unexpectedType(this)
}

private fun PsiClass.getInternalName(): String {
    val containingClass = this.containingClass
    return if (containingClass != null) {
        containingClass.getInternalName() + "$" + this.name
    } else {
        qualifiedName?.replace('.', '/') ?: error("Invalid class name ($this)")
    }
}

private fun byteValue(value: Any?): Int = (value as? Number)?.toByte()?.toInt() ?: 0
private fun intValue(value: Any?): Int = (value as? Number)?.toInt() ?: 0
private fun shortValue(value: Any?): Int = (value as? Number)?.toShort()?.toInt() ?: 0
private fun booleanValue(value: Any?): Int = if (value == true) 1 else 0
private fun charValue(value: Any?): Int = (value as? Char)?.toInt() ?: 0.toChar().toInt()

private fun longValue(value: Any?): Long = (value as? Number)?.toLong() ?: 0
private fun floatValue(value: Any?): Float = (value as? Number)?.toFloat() ?: 0f
private fun doubleValue(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0

private fun loadClass(fqName: String, bytes: ByteArray, annotationClass: Class<*>): Class<*>? {
    class ByteClassLoader(
            urls: Array<out URL>?,
            parent: ClassLoader?,
            val extraClasses: MutableMap<String, ByteArray>,
            predefinedClasses: List<Class<*>>
    ) : URLClassLoader(urls, parent) {
        private val predefinedClasses = predefinedClasses.associateBy { it.canonicalName }
        
        override fun findClass(name: String): Class<*>? {
            return extraClasses.remove(name)?.let {
                defineClass(name, it, 0, it.size)
            } ?: predefinedClasses[name] ?: super.findClass(name)
        }
    }

    val classLoader = ByteClassLoader(emptyArray(), annotationClass.classLoader, hashMapOf(fqName to bytes), listOf())
    return Class.forName(fqName, false, classLoader)
}

private fun unexpectedType(type: String): Nothing = error("Unexpected type: $type")
private fun unexpectedType(type: PsiType): Nothing = unexpectedType(type.presentableText)
private fun unexpectedType(type: Type): Nothing = unexpectedType(type.descriptor)