/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.jet.j2k.ast.*
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType
import org.jetbrains.jet.j2k.TypeConverter
import org.jetbrains.jet.j2k.Converter

private val PRIMITIVE_TYPES_NAMES = JvmPrimitiveType.values().map { it.getName() }

class TypeVisitor(private val converter: Converter) : PsiTypeVisitor<Type>() {

    private val typeConverter: TypeConverter = converter.typeConverter

    override fun visitPrimitiveType(primitiveType: PsiPrimitiveType): Type {
        val name = primitiveType.getCanonicalText()
        return if (name == "void") {
            UnitType()
        }
        else if (PRIMITIVE_TYPES_NAMES.contains(name)) {
            PrimitiveType(Identifier(StringUtil.capitalize(name)).assignNoPrototype())
        }
        else if (name == "null") {
            NullType()
        }
        else {
            PrimitiveType(Identifier(name).assignNoPrototype())
        }
    }

    override fun visitArrayType(arrayType: PsiArrayType): Type {
        return ArrayType(typeConverter.convertType(arrayType.getComponentType()), Nullability.Default, converter.settings)
    }

    override fun visitClassType(classType: PsiClassType): Type {
        val refElement = constructReferenceElement(classType)
        return ClassType(refElement, Nullability.Default, converter.settings)
    }

    private fun constructReferenceElement(classType: PsiClassType): ReferenceElement {
        val typeArgs = convertTypeArgs(classType)

        val psiClass = classType.resolve()
        if (psiClass != null) {
            val javaClassName = psiClass.getQualifiedName()
            val kotlinClassName = toKotlinTypesMap[javaClassName]
            if (kotlinClassName != null) {
                val kotlinShortName = getShortName(kotlinClassName)
                if (kotlinShortName == getShortName(javaClassName!!) && converter.importNames.contains(getPackageName(javaClassName) + ".*")) {
                    converter.importsToAdd?.add(kotlinClassName)
                }
                return ReferenceElement(Identifier(kotlinShortName).assignNoPrototype(), typeArgs).assignNoPrototype()
            }
        }

        if (classType is PsiClassReferenceType) {
            return converter.convertCodeReferenceElement(classType.getReference(), hasExternalQualifier = false, typeArgsConverted = typeArgs)
        }

        return ReferenceElement(Identifier(classType.getClassName() ?: "").assignNoPrototype(), typeArgs).assignNoPrototype()
    }

    private fun getPackageName(className: String): String = className.substring(0, className.lastIndexOf('.'))
    private fun getShortName(className: String): String = className.substring(className.lastIndexOf('.') + 1)

    private fun convertTypeArgs(classType: PsiClassType): List<Type> {
        if (classType.getParameterCount() == 0) {
            return createTypeArgsForRawTypeUsage(classType)
        }
        else {
            return typeConverter.convertTypes(classType.getParameters())
        }
    }

    private fun createTypeArgsForRawTypeUsage(classType: PsiClassType): List<Type> {
        if (classType is PsiClassReferenceType) {
            val targetClass = classType.getReference().resolve() as? PsiClass
            if (targetClass != null) {
                return targetClass.getTypeParameters().map {
                    val superType = it.getSuperTypes().first() // there must be at least one super type always
                    ClassType(constructReferenceElement(superType), Nullability.Default, converter.settings).assignNoPrototype()
                }
            }
        }
        return listOf()
    }

    override fun visitWildcardType(wildcardType: PsiWildcardType): Type {
        return when {
            wildcardType.isExtends() -> OutProjectionType(typeConverter.convertType(wildcardType.getExtendsBound()))
            wildcardType.isSuper() -> InProjectionType(typeConverter.convertType(wildcardType.getSuperBound()))
            else -> StarProjectionType()
        }
    }

    override fun visitEllipsisType(ellipsisType: PsiEllipsisType): Type {
        return VarArgType(typeConverter.convertType(ellipsisType.getComponentType()))
    }

    class object {
        private val toKotlinTypesMap: Map<String, String> = mapOf(
                CommonClassNames.JAVA_LANG_OBJECT to "kotlin.Any",
                CommonClassNames.JAVA_LANG_BYTE to "kotlin.Byte",
                CommonClassNames.JAVA_LANG_CHARACTER to "kotlin.Char",
                CommonClassNames.JAVA_LANG_DOUBLE to "kotlin.Double",
                CommonClassNames.JAVA_LANG_FLOAT to "kotlin.Float",
                CommonClassNames.JAVA_LANG_INTEGER to "kotlin.Int",
                CommonClassNames.JAVA_LANG_LONG to "kotlin.Long",
                CommonClassNames.JAVA_LANG_SHORT to "kotlin.Short",
                CommonClassNames.JAVA_LANG_BOOLEAN to "kotlin.Boolean",
                CommonClassNames.JAVA_LANG_ITERABLE to "kotlin.Iterable",
                CommonClassNames.JAVA_UTIL_ITERATOR to "kotlin.Iterator",
                CommonClassNames.JAVA_UTIL_LIST to "kotlin.List",
                CommonClassNames.JAVA_UTIL_COLLECTION to "kotlin.Collection",
                CommonClassNames.JAVA_UTIL_SET to "kotlin.Set",
                CommonClassNames.JAVA_UTIL_MAP to "kotlin.Map"
        )
    }
}
