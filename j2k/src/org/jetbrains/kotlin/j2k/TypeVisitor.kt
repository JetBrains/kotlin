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
        return when {
            name == "void" -> UnitType()
            PRIMITIVE_TYPES_NAMES.contains(name) -> PrimitiveType(Identifier.withNoPrototype(StringUtil.capitalize(name)))
            name == "null" -> NullType()
            else -> PrimitiveType(Identifier.withNoPrototype(name))
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
            converter.convertToKotlinAnalogIdentifier(javaClassName, mutability)?.let {
                return ReferenceElement(it, typeArgs).assignNoPrototype()
            }

            if (inAnnotationType && javaClassName == "java.lang.Class") {
                val fqName = FqName("kotlin.reflect.KClass")
                val identifier = Identifier.withNoPrototype(fqName.shortName().identifier, imports = listOf(fqName))
                return ReferenceElement(identifier, typeArgs).assignNoPrototype()
            }
        }

        if (classType is PsiClassReferenceType) {
            return converter.convertCodeReferenceElement(classType.reference, hasExternalQualifier = false, typeArgsConverted = typeArgs)
        }

        return ReferenceElement(Identifier.withNoPrototype(classType.className ?: ""), typeArgs).assignNoPrototype()
    }

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
}
