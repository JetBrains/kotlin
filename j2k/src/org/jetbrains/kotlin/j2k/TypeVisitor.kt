/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES as BUILTIN_NAMES

private val PRIMITIVE_TYPES_NAMES = JvmPrimitiveType.values().map { it.javaKeywordName }

class TypeVisitor(
        private val converter: Converter,
        private val topLevelType: PsiType,
        private val topLevelTypeMutability: Mutability,
        private val inAnnotationType: Boolean
) : PsiTypeVisitor<Type>() {

    private val typeConverter: TypeConverter = converter.typeConverter

    //TODO: support for all types
    override fun visitType(type: PsiType) = ErrorType()

    override fun visitPrimitiveType(primitiveType: PsiPrimitiveType): Type {
        val name = primitiveType.canonicalText
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
        return ArrayType(typeConverter.convertType(arrayType.componentType, inAnnotationType = inAnnotationType), Nullability.Default, converter.settings)
    }

    override fun visitClassType(classType: PsiClassType): Type {
        val mutability = if (classType === topLevelType) topLevelTypeMutability else Mutability.Default
        val refElement = constructReferenceElement(classType, mutability)
        return ClassType(refElement, Nullability.Default, converter.settings)
    }

    private fun constructReferenceElement(classType: PsiClassType, mutability: Mutability): ReferenceElement {
        val typeArgs = convertTypeArgs(classType)

        val psiClass = classType.resolve()
        if (psiClass != null) {
            val javaClassName = psiClass.qualifiedName
            val kotlinClassName = (if (mutability.isMutable(converter.settings)) toKotlinMutableTypesMap[javaClassName] else null)
                                  ?: toKotlinTypesMap[javaClassName]
            if (kotlinClassName != null) {
                return ReferenceElement(Identifier(getShortName(kotlinClassName)).assignNoPrototype(), typeArgs).assignNoPrototype()
            }

            if (inAnnotationType && javaClassName == "java.lang.Class") {
                val fqName = FqName("kotlin.reflect.KClass")
                val identifier = Identifier(fqName.shortName().identifier, imports = listOf(fqName)).assignNoPrototype()
                return ReferenceElement(identifier, typeArgs).assignNoPrototype()
            }
        }

        if (classType is PsiClassReferenceType) {
            return converter.convertCodeReferenceElement(classType.reference, hasExternalQualifier = false, typeArgsConverted = typeArgs)
        }

        return ReferenceElement(Identifier(classType.className ?: "").assignNoPrototype(), typeArgs).assignNoPrototype()
    }

    private fun getShortName(className: String): String = className.substringAfterLast('.', className)

    private fun convertTypeArgs(classType: PsiClassType): List<Type> {
        if (classType.parameterCount == 0) {
            return createTypeArgsForRawTypeUsage(classType, Mutability.Default)
        }
        else {
            return typeConverter.convertTypes(classType.parameters)
        }
    }

    private fun createTypeArgsForRawTypeUsage(classType: PsiClassType, mutability: Mutability): List<Type> {
        if (classType is PsiClassReferenceType) {
            val targetClass = classType.reference.resolve() as? PsiClass
            if (targetClass != null) {
                return targetClass.typeParameters.map { StarProjectionType().assignNoPrototype() }
            }
        }
        return listOf()
    }

    override fun visitWildcardType(wildcardType: PsiWildcardType): Type {
        return when {
            wildcardType.isExtends -> OutProjectionType(typeConverter.convertType(wildcardType.extendsBound))
            wildcardType.isSuper -> InProjectionType(typeConverter.convertType(wildcardType.superBound))
            else -> StarProjectionType()
        }
    }

    override fun visitEllipsisType(ellipsisType: PsiEllipsisType): Type {
        return VarArgType(typeConverter.convertType(ellipsisType.componentType, inAnnotationType = inAnnotationType))
    }

    companion object {
        private val toKotlinTypesMap: Map<String, String> = mapOf(
                CommonClassNames.JAVA_LANG_OBJECT to BUILTIN_NAMES.any.asString(),
                CommonClassNames.JAVA_LANG_BYTE to BUILTIN_NAMES._byte.asString(),
                CommonClassNames.JAVA_LANG_CHARACTER to BUILTIN_NAMES._char.asString(),
                CommonClassNames.JAVA_LANG_DOUBLE to BUILTIN_NAMES._double.asString(),
                CommonClassNames.JAVA_LANG_FLOAT to BUILTIN_NAMES._float.asString(),
                CommonClassNames.JAVA_LANG_INTEGER to BUILTIN_NAMES._int.asString(),
                CommonClassNames.JAVA_LANG_LONG to BUILTIN_NAMES._long.asString(),
                CommonClassNames.JAVA_LANG_SHORT to BUILTIN_NAMES._short.asString(),
                CommonClassNames.JAVA_LANG_BOOLEAN to BUILTIN_NAMES._boolean.asString(),
                CommonClassNames.JAVA_LANG_ITERABLE to BUILTIN_NAMES.iterable.asString(),
                CommonClassNames.JAVA_UTIL_ITERATOR to BUILTIN_NAMES.iterator.asString(),
                CommonClassNames.JAVA_UTIL_LIST to BUILTIN_NAMES.list.asString(),
                CommonClassNames.JAVA_UTIL_COLLECTION to BUILTIN_NAMES.collection.asString(),
                CommonClassNames.JAVA_UTIL_SET to BUILTIN_NAMES.set.asString(),
                CommonClassNames.JAVA_UTIL_MAP to BUILTIN_NAMES.map.asString(),
                CommonClassNames.JAVA_UTIL_MAP_ENTRY to BUILTIN_NAMES.mapEntry.asString(),
                java.util.ListIterator::class.java.canonicalName to BUILTIN_NAMES.listIterator.asString()

        )

        val toKotlinMutableTypesMap: Map<String, String> = mapOf(
                CommonClassNames.JAVA_UTIL_ITERATOR to BUILTIN_NAMES.mutableIterator.asString(),
                CommonClassNames.JAVA_UTIL_LIST to BUILTIN_NAMES.mutableList.asString(),
                CommonClassNames.JAVA_UTIL_COLLECTION to BUILTIN_NAMES.mutableCollection.asString(),
                CommonClassNames.JAVA_UTIL_SET to BUILTIN_NAMES.mutableSet.asString(),
                CommonClassNames.JAVA_UTIL_MAP to BUILTIN_NAMES.mutableMap.asString(),
                CommonClassNames.JAVA_UTIL_MAP_ENTRY to BUILTIN_NAMES.mutableMapEntry.asString(),
                java.util.ListIterator::class.java.canonicalName to BUILTIN_NAMES.mutableListIterator.asString()
        )
    }
}
