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

class TypeVisitor(private val converter: TypeConverter, private val importNames: Set<String>, private val classesToImport: MutableSet<String>) : PsiTypeVisitor<Type>() {
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
        return ArrayType(converter.convertType(arrayType.getComponentType()), Nullability.Default, converter.settings)
    }

    override fun visitClassType(classType: PsiClassType): Type {
        val identifier = constructClassTypeIdentifier(classType)
        val resolvedClassTypeParams = createRawTypesForResolvedReference(classType)
        if (classType.getParameterCount() == 0 && resolvedClassTypeParams.size() > 0) {
            val starParamList = ArrayList<Type>()
            if (resolvedClassTypeParams.size() == 1) {
                if ((resolvedClassTypeParams.single() as ClassType).name.name == "Any") {
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
            val javaClassName = psiClass.getQualifiedName()
            val kotlinClassName = toKotlinTypesMap[javaClassName]
            if (kotlinClassName != null) {
                val kotlinShortName = getShortName(kotlinClassName)
                if (kotlinShortName == getShortName(javaClassName!!) && importNames.contains(getPackageName(javaClassName) + ".*")) {
                    classesToImport.add(kotlinClassName)
                }
                return Identifier(kotlinShortName).assignNoPrototype()
            }
        }

        if (classType is PsiClassReferenceType) {
            val reference = classType.getReference()
            if (reference.isQualified()) {
                var result = Identifier.toKotlin(reference.getReferenceName()!!)
                var qualifier = reference.getQualifier()
                while (qualifier != null) {
                    val codeRefElement = qualifier as PsiJavaCodeReferenceElement
                    result = Identifier.toKotlin(codeRefElement.getReferenceName()!!) + "." + result
                    qualifier = codeRefElement.getQualifier()
                }
                return Identifier(result).assignNoPrototype()
            }
        }

        return Identifier(classType.getClassName() ?: "").assignNoPrototype()
    }

    private fun getPackageName(className: String): String = className.substring(0, className.lastIndexOf('.'))
    private fun getShortName(className: String): String = className.substring(className.lastIndexOf('.') + 1)

    private fun createRawTypesForResolvedReference(classType: PsiClassType): List<Type> {
        val typeParams = LinkedList<Type>()
        if (classType is PsiClassReferenceType) {
            val resolve = classType.getReference().resolve()
            if (resolve is PsiClass) {
                for (typeParam in resolve.getTypeParameters()) {
                    val superTypes = typeParam.getSuperTypes()
                    val boundType = if (superTypes.size > 0) {
                        ClassType(constructClassTypeIdentifier(superTypes[0]),
                                  converter.convertTypes(superTypes[0].getParameters()),
                                  Nullability.Default,
                                  converter.settings)
                    }
                    else {
                        StarProjectionType()
                    }
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
