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

import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Annotation
import org.jetbrains.kotlin.load.java.components.JavaAnnotationTargetMapper
import java.lang.annotation.ElementType
import java.lang.annotation.Target

class AnnotationConverter(private val converter: Converter) {
    private val annotationsToRemove: Set<String> = (NullableNotNullManager.getInstance(converter.project).getNotNulls()
            + NullableNotNullManager.getInstance(converter.project).getNullables()
            + listOf(CommonClassNames.JAVA_LANG_OVERRIDE, javaClass<ElementType>().name)).toSet()

    public fun isImportRequired(annotationName: String): Boolean {
        return annotationName in annotationsToRemove || annotationName == javaClass<Target>().name
    }

    public fun convertAnnotations(owner: PsiModifierListOwner): Annotations
            = convertAnnotationsOnly(owner) + convertModifiersToAnnotations(owner)

    private fun convertAnnotationsOnly(owner: PsiModifierListOwner): Annotations {
        val modifierList = owner.getModifierList()
        val annotations = modifierList?.getAnnotations()?.filter { it.getQualifiedName() !in annotationsToRemove }

        var convertedAnnotations: List<Annotation> = if (annotations != null && annotations.isNotEmpty()) {
            val newLines = if (!modifierList!!.isInSingleLine()) {
                true
            }
            else {
                var child: PsiElement? = modifierList
                while (true) {
                    child = child!!.getNextSibling()
                    if (child == null || child.getTextLength() != 0) break
                }
                if (child is PsiWhiteSpace) !child.isInSingleLine() else false
            }

            annotations.map { convertAnnotation(it, withAt = owner is PsiLocalVariable, newLineAfter = newLines) }.filterNotNull() //TODO: '@' is also needed for local classes
        }
        else {
            listOf()
        }

        if (owner is PsiDocCommentOwner) {
            val deprecatedAnnotation = convertDeprecatedJavadocTag(owner)
            if (deprecatedAnnotation != null) {
                convertedAnnotations += deprecatedAnnotation
            }
        }

        return Annotations(convertedAnnotations).assignNoPrototype()
    }

    private fun convertDeprecatedJavadocTag(element: PsiDocCommentOwner): Annotation? {
        val deprecatedTag = element.getDocComment()?.findTagByName("deprecated") ?: return null
        val deferredExpression = converter.deferredElement<Expression> {
            LiteralExpression("\"" + StringUtil.escapeStringCharacters(deprecatedTag.content()) + "\"").assignNoPrototype()
        }
        return Annotation(Identifier("deprecated").assignPrototype(deprecatedTag.getNameElement()),
                          listOf(null to deferredExpression), withAt = false, newLineAfter = true)
                .assignPrototype(deprecatedTag)
    }

    private fun convertModifiersToAnnotations(owner: PsiModifierListOwner): Annotations {
        val list = MODIFIER_TO_ANNOTATION
                .filter { owner.hasModifierProperty(it.first) }
                .map { Annotation(Identifier(it.second).assignNoPrototype(), listOf(), withAt = false, newLineAfter = false).assignNoPrototype() }
        return Annotations(list).assignNoPrototype()
    }

    private val MODIFIER_TO_ANNOTATION = listOf(
            PsiModifier.SYNCHRONIZED to "synchronized",
            PsiModifier.VOLATILE to "volatile",
            PsiModifier.STRICTFP to "strictfp",
            PsiModifier.TRANSIENT to "transient"
    )

    private fun mapTargetByName(expr: PsiReferenceExpression): Set<KotlinTarget> {
        return expr.referenceName?.let { JavaAnnotationTargetMapper.mapJavaTargetArgumentByName(it) } ?: emptySet()
    }

    public fun convertAnnotation(annotation: PsiAnnotation, withAt: Boolean, newLineAfter: Boolean): Annotation? {
        val qualifiedName = annotation.getQualifiedName()
        if (qualifiedName == CommonClassNames.JAVA_LANG_DEPRECATED && annotation.getParameterList().getAttributes().isEmpty()) {
            val deferredExpression = converter.deferredElement<Expression> { LiteralExpression("\"\"").assignNoPrototype() }
            return Annotation(Identifier("deprecated").assignNoPrototype(), listOf(null to deferredExpression), withAt, newLineAfter).assignPrototype(annotation) //TODO: insert comment
        }
        if (qualifiedName == CommonClassNames.JAVA_LANG_ANNOTATION_TARGET) {
            val attributes = annotation.parameterList.attributes
            val arguments: Set<KotlinTarget>
            if (attributes.isEmpty()) {
                arguments = setOf<KotlinTarget>()
            }
            else {
                val value = attributes[0].value
                arguments = when (value) {
                    is PsiArrayInitializerMemberValue -> value.getInitializers().filterIsInstance<PsiReferenceExpression>()
                            .flatMap { mapTargetByName(it) }
                            .toSet()
                    is PsiReferenceExpression -> mapTargetByName(value)
                    else -> setOf<KotlinTarget>()
                }
            }
            val deferredExpressionList = arguments.map {
                val name = it.name()
                null to converter.deferredElement<Expression> {
                    QualifiedExpression(Identifier("AnnotationTarget", false).assignNoPrototype(),
                                        Identifier(name, false).assignNoPrototype())
                }
            }
            return Annotation(Identifier("target").assignNoPrototype(),
                              deferredExpressionList, withAt, newLineAfter).assignPrototype(annotation)
        }

        val nameRef = annotation.getNameReferenceElement()
        val name = Identifier((nameRef ?: return null).getText()!!).assignPrototype(nameRef)
        val annotationClass = nameRef.resolve() as? PsiClass
        val arguments = annotation.getParameterList().getAttributes().flatMap {
            val parameterName = it.getName() ?: "value"
            val method = annotationClass?.findMethodsByName(parameterName, false)?.firstOrNull()
            val expectedType = method?.getReturnType()

            val attrName = it.getName()?.let { Identifier(it).assignNoPrototype() }
            val value = it.getValue()

            val isVarArg = parameterName == "value" /* converted to vararg in Kotlin */
            val attrValues = convertAttributeValue(value, expectedType, isVarArg, it.getName() == null)

            attrValues.map { attrName to converter.deferredElement(it) }
        }
        return Annotation(name, arguments, withAt, newLineAfter).assignPrototype(annotation)
    }

    public fun convertAnnotationMethodDefault(method: PsiAnnotationMethod): DeferredElement<Expression>? {
        val value = method.getDefaultValue() ?: return null
        return converter.deferredElement(convertAttributeValue(value, method.getReturnType(), false, false).single())
    }

    private fun convertAttributeValue(value: PsiAnnotationMemberValue?, expectedType: PsiType?, isVararg: Boolean, isUnnamed: Boolean): List<(CodeConverter) -> Expression> {
        return when (value) {
            is PsiExpression -> listOf({ codeConverter -> convertExpressionValue(codeConverter, value, expectedType) })

            is PsiArrayInitializerMemberValue -> {
                val componentType = (expectedType as? PsiArrayType)?.getComponentType()
                val componentGenerators = value.getInitializers().map { convertAttributeValue(it, componentType, false, true).single() }
                if (isVararg && isUnnamed) {
                    componentGenerators
                }
                else {
                    listOf({ codeConverter -> convertArrayInitializerValue(codeConverter, value, componentGenerators, expectedType, isVararg) })
                }
            }

            else -> listOf({ codeConverter -> DummyStringExpression(value?.getText() ?: "").assignPrototype(value) })
        }
    }

    private fun convertExpressionValue(codeConverter: CodeConverter, value: PsiExpression, expectedType: PsiType?): Expression {
        val expression = if (value is PsiClassObjectAccessExpression) {
            val typeElement = converter.convertTypeElement(value.getOperand())
            ClassLiteralExpression(typeElement.type.toNotNullType())
        }
        else {
            codeConverter.convertExpression(value, expectedType)
        }
        return expression.assignPrototype(value)
    }

    private fun convertArrayInitializerValue(
            codeConverter: CodeConverter,
            value: PsiArrayInitializerMemberValue,
            componentGenerators: List<(CodeConverter) -> Expression>,
            expectedType: PsiType?,
            isVararg: Boolean
    ): Expression {
        val expectedTypeConverted = converter.typeConverter.convertType(expectedType)
        return if (expectedTypeConverted is ArrayType) {
            val array = createArrayInitializerExpression(expectedTypeConverted, componentGenerators.map { it(codeConverter) }, needExplicitType = false)
            if (isVararg) {
                StarExpression(array.assignNoPrototype()).assignPrototype(value)
            }
            else {
                array.assignPrototype(value)
            }
        }
        else {
            DummyStringExpression(value.getText()!!).assignPrototype(value)
        }
    }
}
