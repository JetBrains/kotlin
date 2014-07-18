/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.*
import org.jetbrains.jet.j2k.ast.*
import com.intellij.codeInsight.NullableNotNullManager

class AnnotationConverter(private val converter: Converter) {
    public val annotationsToRemove: Set<String> = (NullableNotNullManager.getInstance(converter.project).getNotNulls()
            + NullableNotNullManager.getInstance(converter.project).getNullables()
            + listOf(CommonClassNames.JAVA_LANG_OVERRIDE)).toSet()

    public fun convertAnnotations(owner: PsiModifierListOwner): Annotations
            = (convertAnnotationsOnly(owner) + convertModifiersToAnnotations(owner)).assignNoPrototype()

    private fun convertAnnotationsOnly(owner: PsiModifierListOwner): Annotations {
        val modifierList = owner.getModifierList()
        val annotations = modifierList?.getAnnotations()?.filter { it.getQualifiedName() !in annotationsToRemove }
        if (annotations == null || annotations.isEmpty()) return Annotations.Empty

        val newLines = run {
            if (!modifierList!!.isInSingleLine()) {
                true
            }
            else {
                var child: PsiElement? = modifierList
                while (true) {
                    child = child!!.getNextSibling()
                    if (child == null || child!!.getTextLength() != 0) break
                }
                if (child is PsiWhiteSpace) !child!!.isInSingleLine() else false
            }
        }

        val list = annotations.map { convertAnnotation(it, owner is PsiLocalVariable, newLines) }.filterNotNull() //TODO: brackets are also needed for local classes
        return Annotations(list).assignNoPrototype()
    }

    private fun convertModifiersToAnnotations(owner: PsiModifierListOwner): Annotations {
        val list = MODIFIER_TO_ANNOTATION
                .filter { owner.hasModifierProperty(it.first) }
                .map { Annotation(Identifier(it.second).assignNoPrototype(), listOf(), false, false).assignNoPrototype() }
        return Annotations(list).assignNoPrototype()
    }

    private val MODIFIER_TO_ANNOTATION = listOf(
            PsiModifier.SYNCHRONIZED to "synchronized",
            PsiModifier.VOLATILE to "volatile",
            PsiModifier.STRICTFP to "strictfp",
            PsiModifier.TRANSIENT to "transient"
    )

    public fun convertAnnotation(annotation: PsiAnnotation, brackets: Boolean, newLineAfter: Boolean): Annotation? {
        val qualifiedName = annotation.getQualifiedName()
        if (qualifiedName == CommonClassNames.JAVA_LANG_DEPRECATED && annotation.getParameterList().getAttributes().isEmpty()) {
            return Annotation(Identifier("deprecated").assignNoPrototype(), listOf(null to LiteralExpression("\"\"").assignNoPrototype()), brackets, newLineAfter).assignPrototype(annotation) //TODO: insert comment
        }

        val nameRef = annotation.getNameReferenceElement()
        val name = Identifier((nameRef ?: return null).getText()!!).assignPrototype(nameRef)
        val annotationClass = nameRef!!.resolve() as? PsiClass
        val lastMethod = annotationClass?.getMethods()?.lastOrNull()
        val arguments = annotation.getParameterList().getAttributes().flatMap {
            val method = annotationClass?.findMethodsByName(it.getName() ?: "value", false)?.firstOrNull()
            val expectedType = method?.getReturnType()

            val attrName = it.getName()?.let { Identifier(it).assignNoPrototype() }
            val value = it.getValue()

            val isVarArg = method == lastMethod /* converted to vararg in Kotlin */
            val attrValues = convertAttributeValue(value, expectedType, isVarArg, it.getName() == null)

            attrValues.map { attrName to it }
        }
        return Annotation(name, arguments, brackets, newLineAfter).assignPrototype(annotation)
    }

    public fun convertAnnotationMethodDefault(method: PsiAnnotationMethod): Expression? {
        val value = method.getDefaultValue() ?: return null
        return convertAttributeValue(value, method.getReturnType(), false, false).single()
    }

    private fun convertAttributeValue(value: PsiAnnotationMemberValue?, expectedType: PsiType?, isVararg: Boolean, isUnnamed: Boolean): List<Expression> {
        return when (value) {
            is PsiExpression -> listOf(converter.convertExpression(value as? PsiExpression, expectedType).assignPrototype(value))

            is PsiArrayInitializerMemberValue -> {
                val componentType = (expectedType as? PsiArrayType)?.getComponentType()
                val componentsConverted = value.getInitializers().map { convertAttributeValue(it, componentType, false, true).single() }
                if (isVararg && isUnnamed) {
                    componentsConverted
                }
                else {
                    val expectedTypeConverted = converter.typeConverter.convertType(expectedType)
                    if (expectedTypeConverted is ArrayType) {
                        val array = createArrayInitializerExpression(expectedTypeConverted, componentsConverted, needExplicitType = false)
                        if (isVararg) {
                            listOf(StarExpression(array.assignNoPrototype()).assignPrototype(value))
                        }
                        else {
                            listOf(array.assignPrototype(value))
                        }
                    }
                    else {
                        listOf(DummyStringExpression(value.getText()!!).assignPrototype(value))
                    }
                }
            }

            else -> listOf(DummyStringExpression(value?.getText() ?: "").assignPrototype(value))
        }
    }

}