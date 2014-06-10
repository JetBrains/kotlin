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
import java.util.LinkedList
import com.intellij.openapi.util.text.StringUtil
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType
import org.jetbrains.jet.j2k.TypeConverter

private val PRIMITIVE_TYPES_NAMES = JvmPrimitiveType.values().map { it.getName() }

class TypeVisitor(private val converter: TypeConverter) : PsiTypeVisitor<Type>() {
    override fun visitPrimitiveType(primitiveType: PsiPrimitiveType): Type {
        val name = primitiveType.getCanonicalText()
        return if (name == "void") {
            Type.Unit
        }
        else if (PRIMITIVE_TYPES_NAMES.contains(name)) {
            PrimitiveType(Identifier(StringUtil.capitalize(name)))
        }
        else {
            PrimitiveType(Identifier(name))
        }
    }

    override fun visitArrayType(arrayType: PsiArrayType): Type {
        return ArrayType(converter.convertType(arrayType.getComponentType()), Nullability.Default, converter.settings)
    }

    override fun visitClassType(classType: PsiClassType): Type {
        val identifier = constructClassTypeIdentifier(classType)
        val resolvedClassTypeParams = createRawTypesForResolvedReference(classType)
        if (classType.getParameterCount() == 0 && resolvedClassTypeParams.size() > 0) {
            val starParamList = ArrayList<Type>()
            if (resolvedClassTypeParams.size() == 1) {
                if ((resolvedClassTypeParams.single() as ClassType).`type`.name == "Any") {
                    starParamList.add(StarProjectionType())
                    return ClassType(identifier, starParamList, Nullability.Default, converter.settings)
                }
                else {
                    return ClassType(identifier, resolvedClassTypeParams, Nullability.Default, converter.settings)
                }
            }
            else {
                return ClassType(identifier, resolvedClassTypeParams, Nullability.Default, converter.settings)
            }
        }
        else {
            return ClassType(identifier, converter.convertTypes(classType.getParameters()), Nullability.Default, converter.settings)
        }
    }

    private fun constructClassTypeIdentifier(classType: PsiClassType): Identifier {
        val psiClass = classType.resolve()
        if (psiClass != null) {
            val qualifiedName = psiClass.getQualifiedName()
            when(qualifiedName) {
                CommonClassNames.JAVA_LANG_ITERABLE -> return Identifier(CommonClassNames.JAVA_LANG_ITERABLE)
                CommonClassNames.JAVA_UTIL_ITERATOR -> return Identifier(CommonClassNames.JAVA_UTIL_ITERATOR)
                CommonClassNames.JAVA_UTIL_LIST -> return Identifier("MutableList")
            }
        }

        val classTypeName = createQualifiedName(classType)
        if (classTypeName.isEmpty()) {
            return Identifier(getClassTypeName(classType))
        }

        return Identifier(classTypeName)
    }

    private fun createRawTypesForResolvedReference(classType: PsiClassType): List<Type> {
        val typeParams = LinkedList<Type>()
        if (classType is PsiClassReferenceType) {
            val reference = classType.getReference()
            val resolve = reference.resolve()
            if (resolve is PsiClass) {
                for (typeParam in resolve.getTypeParameters()) {
                    val superTypes = typeParam.getSuperTypes()
                    val boundType = if (superTypes.size > 0)
                        ClassType(Identifier(getClassTypeName(superTypes[0])),
                                  converter.convertTypes(superTypes[0].getParameters()),
                                  Nullability.Default,
                                  converter.settings)
                    else
                        StarProjectionType()
                    typeParams.add(boundType)
                }
            }
        }

        return typeParams
    }

    override fun visitWildcardType(wildcardType: PsiWildcardType): Type {
        return when {
            wildcardType.isExtends() -> OutProjectionType(converter.convertType(wildcardType.getExtendsBound()))
            wildcardType.isSuper() -> InProjectionType(converter.convertType(wildcardType.getSuperBound()))
            else -> StarProjectionType()
        }
    }

    override fun visitEllipsisType(ellipsisType: PsiEllipsisType): Type {
        return VarArgType(converter.convertType(ellipsisType.getComponentType()))
    }

    private fun createQualifiedName(classType: PsiClassType): String {
        if (classType is PsiClassReferenceType) {
            val reference = classType.getReference()
            if (reference.isQualified()) {
                var result = Identifier(reference.getReferenceName()!!).toKotlin()
                var qualifier = reference.getQualifier()
                while (qualifier != null) {
                    val codeRefElement = qualifier as PsiJavaCodeReferenceElement
                    result = Identifier(codeRefElement.getReferenceName()!!).toKotlin() + "." + result
                    qualifier = codeRefElement.getQualifier()
                }
                return result
            }
        }

        return ""
    }

    private fun getClassTypeName(classType: PsiClassType): String {
        var canonicalTypeStr: String? = classType.getCanonicalText()
        return when(canonicalTypeStr) {
            CommonClassNames.JAVA_LANG_OBJECT -> "Any"
            CommonClassNames.JAVA_LANG_BYTE -> "Byte"
            CommonClassNames.JAVA_LANG_CHARACTER -> "Char"
            CommonClassNames.JAVA_LANG_DOUBLE -> "Double"
            CommonClassNames.JAVA_LANG_FLOAT -> "Float"
            CommonClassNames.JAVA_LANG_INTEGER -> "Int"
            CommonClassNames.JAVA_LANG_LONG -> "Long"
            CommonClassNames.JAVA_LANG_SHORT -> "Short"
            CommonClassNames.JAVA_LANG_BOOLEAN -> "Boolean"

            else -> classType.getClassName() ?: classType.getCanonicalText()
        }
    }
}
