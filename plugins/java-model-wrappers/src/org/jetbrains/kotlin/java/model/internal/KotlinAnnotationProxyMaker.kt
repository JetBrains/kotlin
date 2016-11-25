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
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.types.toJeType
import sun.reflect.annotation.AnnotationParser
import sun.reflect.annotation.ExceptionProxy
import java.io.ObjectInputStream
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.util.*
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror

class KotlinAnnotationProxyMaker(val annotation: PsiAnnotation, val annotationClass: PsiClass, val annotationType: Class<out Annotation>) {
    fun generate(): Annotation {
        return AnnotationParser.annotationForMap(annotationType, getAllValuesForParser(getAllPsiValues()))
    }
    
    private data class AnnotationParameterData(
            val method: PsiAnnotationMethod, 
            val value: PsiAnnotationMemberValue, 
            val jMethod: Method)
    
    private fun getAllPsiValues(): List<AnnotationParameterData> {
        val values = mutableListOf<AnnotationParameterData>()
        for (method in annotationClass.methods) {
            if (method !is PsiAnnotationMethod) continue
            if (method.returnType == null) continue
            
            val jMethod = try { annotationType.getMethod(method.name) } catch (e : NoSuchMethodException) { continue }
            
            val value = annotation.findAttributeValue(method.name) ?: method.defaultValue ?: continue
            values += AnnotationParameterData(method, value, jMethod)
        }
        return values
    }
    
    private fun getAllValuesForParser(values: List<AnnotationParameterData>): Map<String, Any?> {
        val parserValues = mutableMapOf<String, Any?>()
        val evaluator = JavaPsiFacade.getInstance(annotation.project).constantEvaluationHelper
        for ((method, value, jMethod) in values) {
            val jReturnType = jMethod.returnType ?: unexpectedType("no return type for ${jMethod.name}")
            parserValues.put(method.name, getConstantValue(value, method.returnType!!, jReturnType, evaluator))
        }
        return parserValues
    }
}

private val JAVA_LANG_STRING = "java.lang.String"

private fun getConstantValue(
        psiValue: PsiAnnotationMemberValue,
        returnType: PsiType,
        jReturnType: Class<*>,
        evaluator: PsiConstantEvaluationHelper
): Any? {
    val manager = psiValue.manager
    
    when {
        returnType == PsiType.NULL || returnType == PsiType.VOID -> unexpectedType("void")
        returnType.fqName == JAVA_LANG_STRING -> return (psiValue as? PsiExpression)?.calcConstantValue(evaluator)
        jReturnType.isAnnotation -> {
            if (psiValue !is PsiAnnotation) error("psiValue is not a PsiAnnotation")
            val annotationClass = PsiTypesUtil.getPsiClass(returnType) ?: error("Can't resolve type $returnType")
            @Suppress("UNCHECKED_CAST")
            val annotation = KotlinAnnotationProxyMaker(psiValue, annotationClass, jReturnType as Class<out Annotation>)
            return jReturnType.cast(annotation.generate())
        }
        jReturnType.isArray -> {
            val jComponentType = jReturnType.componentType ?: unexpectedType("no component type for $jReturnType")
            if (returnType !is PsiArrayType) unexpectedType(returnType)
            
            val arrayValues = when (psiValue) {
                is PsiArrayInitializerMemberValue -> psiValue.initializers.toList()
                else -> listOf(psiValue)
            }

            if (jComponentType.isPrimitive
                || jComponentType.isAnnotation
                || jComponentType.isEnum
                || jComponentType.canonicalName == JAVA_LANG_STRING
            ) {
                val arr = Array.newInstance(jComponentType, arrayValues.size)
                arrayValues.forEachIndexed { i, componentPsiValue ->
                    val componentValue = getConstantValue(componentPsiValue, returnType.componentType, jComponentType, evaluator)
                    try { Array.set(arr, i, componentValue) } catch (e: IllegalArgumentException) { return null }
                }
                return arr
            } else {
                val typeMirrors = arrayValues.map { getObjectType(it).toJeType(manager) }
                return MirroredTypesExceptionProxy(Collections.unmodifiableList(typeMirrors))
            }
        }
        jReturnType.isEnum -> {
            val enumConstant = (psiValue.originalElement as? PsiReference)?.resolve() as? PsiEnumConstant 
                    ?: error("$psiValue can not be resolved to enum constant")
            return AnnotationUtil.createEnumValue(jReturnType, enumConstant.name)
        }
        else -> return if (returnType is PsiClassType) {
            val type = getObjectType(psiValue).toJeType(manager)
            MirroredTypeExceptionProxy(type)
        } else {
            castPrimitiveValue(returnType, (psiValue as? PsiExpression)?.calcConstantValue(evaluator))
        }
    }
}

private val PsiType.fqName: String?
    get() = (this as? PsiClassType)?.resolve()?.qualifiedName

private fun getObjectType(value: PsiAnnotationMemberValue): PsiType {
    when (value) {
        is PsiClassObjectAccessExpression -> return value.operand.type
        is PsiReference -> {
            val resolvedElement = value.resolve()
            if (resolvedElement is PsiField && resolvedElement.isStatic && resolvedElement.isFinal) {
                val initializer = resolvedElement.initializer
                if (initializer != null) {
                    return getObjectType(initializer)
                }
            }
        }
    }
    
    throw IllegalArgumentException("Illegal value type: ${value.javaClass}")
}

private fun castPrimitiveValue(type: PsiType, value: Any?): Any = when (type) {
    PsiType.BYTE -> byteValue(value)
    PsiType.SHORT -> shortValue(value)
    PsiType.INT -> intValue(value)
    PsiType.CHAR -> charValue(value)
    PsiType.BOOLEAN -> booleanValue(value)
    PsiType.LONG -> longValue(value)
    PsiType.FLOAT -> floatValue(value)
    PsiType.DOUBLE -> doubleValue(value)
    else -> unexpectedType(type)
}

private fun byteValue(value: Any?): Byte = (value as? Number)?.toByte() ?: 0
private fun intValue(value: Any?): Int = (value as? Number)?.toInt() ?: 0
private fun shortValue(value: Any?): Short = (value as? Number)?.toShort() ?: 0
private fun booleanValue(value: Any?): Boolean = value == true
private fun charValue(value: Any?): Char = value as? Char ?: 0.toChar()

private fun longValue(value: Any?): Long = (value as? Number)?.toLong() ?: 0
private fun floatValue(value: Any?): Float = (value as? Number)?.toFloat() ?: 0f
private fun doubleValue(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0

private fun unexpectedType(type: String): Nothing = error("Unexpected type: $type")
private fun unexpectedType(type: PsiType): Nothing = unexpectedType(type.presentableText)

private class MirroredTypeExceptionProxy(@Transient private var type: TypeMirror?) : ExceptionProxy() {
    private val typeString = type.toString()

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun hashCode() = (if (type != null) type else typeString)?.hashCode() ?: 0
    override fun equals(other: Any?) = type != null && other is MirroredTypeExceptionProxy && type == other.type
    override fun toString() = typeString
    
    override fun generateException() = MirroredTypeException(type)

    // Explicitly set all transient fields.
    private fun readObject(s: ObjectInputStream) {
        s.defaultReadObject()
        type = null
    }

    companion object {
        private const val serialVersionUID: Long = 269
    }
}

private class MirroredTypesExceptionProxy(@Transient private var types: List<TypeMirror>?) : ExceptionProxy() {
    private val typeStrings: String = types.toString()

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun hashCode() = (if (types != null) types else typeStrings)?.hashCode() ?: 0
    override fun equals(other: Any?) = types != null && other is MirroredTypesExceptionProxy && types == other.types
    override fun toString() = typeStrings

    override fun generateException() = MirroredTypesException(types)

    // Explicitly set all transient fields.
    private fun readObject(s: ObjectInputStream) {
        s.defaultReadObject()
        types = null
    }

    companion object {
        private const val serialVersionUID: Long = 269
    }
}