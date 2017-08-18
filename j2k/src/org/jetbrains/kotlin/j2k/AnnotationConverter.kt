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
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Annotation
import org.jetbrains.kotlin.load.java.components.JavaAnnotationTargetMapper
import org.jetbrains.kotlin.name.FqName
import java.lang.annotation.ElementType
import java.lang.annotation.Target

class AnnotationConverter(private val converter: Converter) {
    private val annotationsToRemove: Set<String> = (NullableNotNullManager.getInstance(converter.project).notNulls
                                                    + NullableNotNullManager.getInstance(converter.project).nullables
                                                    + listOf(CommonClassNames.JAVA_LANG_OVERRIDE, ElementType::class.java.name, SuppressWarnings::class.java.name)).toSet()

    fun isImportNotRequired(fqName: FqName): Boolean {
        val nameAsString = fqName.asString()
        return nameAsString in annotationsToRemove || nameAsString == Target::class.java.name
    }

    fun convertAnnotations(owner: PsiModifierListOwner, target: AnnotationUseTarget? = null): Annotations
            = convertAnnotationsOnly(owner, target) + convertModifiersToAnnotations(owner, target)

    private fun convertAnnotationsOnly(owner: PsiModifierListOwner, target: AnnotationUseTarget?): Annotations {
        val modifierList = owner.modifierList
        val annotations = modifierList?.annotations?.filter { it.qualifiedName !in annotationsToRemove }

        var convertedAnnotations: List<Annotation> = if (annotations != null && annotations.isNotEmpty()) {
            val newLines = if (!modifierList.isInSingleLine()) {
                true
            }
            else {
                var child: PsiElement? = modifierList
                while (true) {
                    child = child!!.nextSibling
                    if (child == null || child.textLength != 0) break
                }
                if (child is PsiWhiteSpace) !child.isInSingleLine() else false
            }

            annotations.mapNotNull { convertAnnotation(it, newLineAfter = newLines, target = target) }
        }
        else {
            listOf()
        }

        if (owner is PsiDocCommentOwner) {
            val deprecatedAnnotation = convertDeprecatedJavadocTag(owner, target)
            if (deprecatedAnnotation != null) {
                convertedAnnotations = convertedAnnotations.filter { it.name.name != deprecatedAnnotation.name.name }
                convertedAnnotations += deprecatedAnnotation
            }
        }

        return Annotations(convertedAnnotations).assignNoPrototype()
    }

    private fun convertDeprecatedJavadocTag(element: PsiDocCommentOwner, target: AnnotationUseTarget?): Annotation? {
        val deprecatedTag = element.docComment?.findTagByName("deprecated") ?: return null
        val deferredExpression = converter.deferredElement<Expression> {
            LiteralExpression("\"" + StringUtil.escapeStringCharacters(deprecatedTag.content()) + "\"").assignNoPrototype()
        }
        val identifier = Identifier("Deprecated").assignPrototype(deprecatedTag.nameElement)
        return Annotation(identifier,
                          listOf(null to deferredExpression),
                          true,
                          effectiveAnnotationUseTarget(identifier.name, target))
                .assignPrototype(deprecatedTag)
    }

    private fun convertModifiersToAnnotations(owner: PsiModifierListOwner, target: AnnotationUseTarget?): Annotations {
        val list = MODIFIER_TO_ANNOTATION
                .filter { owner.hasModifierProperty(it.first) }
                .map {
                    Annotation(Identifier.withNoPrototype(it.second),
                               listOf(),
                               newLineAfter = false,
                               target = target
                    ).assignNoPrototype()
                }
        return Annotations(list).assignNoPrototype()
    }

    private val MODIFIER_TO_ANNOTATION = listOf(
            PsiModifier.SYNCHRONIZED to "Synchronized",
            PsiModifier.VOLATILE to "Volatile",
            PsiModifier.STRICTFP to "Strictfp",
            PsiModifier.TRANSIENT to "Transient"
    )

    private fun mapTargetByName(expr: PsiReferenceExpression): Set<KotlinTarget> {
        return expr.referenceName?.let { JavaAnnotationTargetMapper.mapJavaTargetArgumentByName(it) } ?: emptySet()
    }

    private fun effectiveAnnotationUseTarget(name: String, target: AnnotationUseTarget?): AnnotationUseTarget? =
            when {
                name == "Deprecated" &&
                (target == AnnotationUseTarget.Param || target == AnnotationUseTarget.Field) -> null
                else -> target
            }

    fun convertAnnotation(annotation: PsiAnnotation, newLineAfter: Boolean, target: AnnotationUseTarget? = null): Annotation? {
        val (name, arguments) = convertAnnotationValue(annotation) ?: return null
        return Annotation(name, arguments, newLineAfter, effectiveAnnotationUseTarget(name.name, target)).assignPrototype(annotation, CommentsAndSpacesInheritance.NO_SPACES)
    }

    private fun convertAnnotationValue(annotation: PsiAnnotation): Pair<Identifier, List<Pair<Identifier?, DeferredElement<Expression>>>>? {
        val qualifiedName = annotation.qualifiedName
        if (qualifiedName == CommonClassNames.JAVA_LANG_DEPRECATED && annotation.parameterList.attributes.isEmpty()) {
            val deferredExpression = converter.deferredElement<Expression> { LiteralExpression("\"\"").assignNoPrototype() }
            return Identifier.withNoPrototype("Deprecated") to listOf(null to deferredExpression) //TODO: insert comment
        }
        if (qualifiedName == CommonClassNames.JAVA_LANG_ANNOTATION_TARGET) {
            val attributes = annotation.parameterList.attributes
            val arguments: Set<KotlinTarget> = if (attributes.isEmpty()) {
                setOf()
            }
            else {
                val value = attributes[0].value
                when (value) {
                    is PsiArrayInitializerMemberValue -> value.initializers.filterIsInstance<PsiReferenceExpression>()
                            .flatMap { mapTargetByName(it) }
                            .toSet()
                    is PsiReferenceExpression -> mapTargetByName(value)
                    else -> setOf()
                }
            }
            val deferredExpressionList = arguments.map {
                val name = it.name
                null to converter.deferredElement<Expression> {
                    QualifiedExpression(Identifier.withNoPrototype("AnnotationTarget", isNullable = false),
                                        Identifier.withNoPrototype(name, isNullable = false),
                                        null)
                }
            }
            return Identifier.withNoPrototype("Target") to deferredExpressionList
        }

        val nameRef = annotation.nameReferenceElement
        val name = Identifier((nameRef ?: return null).text!!).assignPrototype(nameRef)
        val annotationClass = nameRef.resolve() as? PsiClass
        val arguments = annotation.parameterList.attributes.flatMap {
            val parameterName = it.name ?: "value"
            val method = annotationClass?.findMethodsByName(parameterName, false)?.firstOrNull()
            val expectedType = method?.returnType

            val attrName = it.name?.let { Identifier.withNoPrototype(it) }
            val value = it.value

            val isVarArg = parameterName == "value" /* converted to vararg in Kotlin */
            val attrValues = convertAttributeValue(value, expectedType, isVarArg, it.name == null)

            attrValues.map { attrName to converter.deferredElement(it) }
        }
        return name to arguments
    }

    fun convertAnnotationMethodDefault(method: PsiAnnotationMethod): DeferredElement<Expression>? {
        val value = method.defaultValue ?: return null
        val returnType = method.returnType
        if (returnType is PsiArrayType && value !is PsiArrayInitializerMemberValue) {
            return converter.deferredElement { codeConverter ->
                val convertedType = converter.typeConverter.convertType(returnType) as ArrayType
                val convertAttributeValue = convertAttributeValue(value, returnType.componentType, false, false)
                createArrayExpression(codeConverter, convertAttributeValue.toList(), convertedType, false)
            }
        }
        return converter.deferredElement(convertAttributeValue(value, returnType, false, false).single())
    }

    private fun convertAttributeValue(value: PsiAnnotationMemberValue?, expectedType: PsiType?, isVararg: Boolean, isUnnamed: Boolean): List<(CodeConverter) -> Expression> {
        return when (value) {
            is PsiExpression -> listOf({ codeConverter -> convertExpressionValue(codeConverter, value, expectedType, isVararg) })

            is PsiArrayInitializerMemberValue -> {
                val componentType = (expectedType as? PsiArrayType)?.componentType
                val componentGenerators = value.initializers.map { convertAttributeValue(it, componentType, false, true).single() }
                if (isVararg && isUnnamed) {
                    componentGenerators
                }
                else {
                    listOf({ codeConverter ->
                               convertArrayInitializerValue(codeConverter, value.text, componentGenerators, expectedType, isVararg)
                                       .assignPrototype(value)
                           })
                }
            }

            is PsiAnnotation -> {
                listOf({ _ ->
                           val (name, arguments) = convertAnnotationValue(value)!!
                           AnnotationConstructorCall(name, arguments).assignPrototype(value)
                       })
            }
            else -> listOf({ _ -> DummyStringExpression(value?.text ?: "").assignPrototype(value) })
        }
    }

    private fun convertExpressionValue(codeConverter: CodeConverter, value: PsiExpression, expectedType: PsiType?, isVararg: Boolean): Expression {
        val expression = if (value is PsiClassObjectAccessExpression) {
            val type = converter.convertTypeElement(value.operand, Nullability.NotNull)
            ClassLiteralExpression(type)
        }
        else {
            codeConverter.convertExpression(value, expectedType)
        }.assignPrototype(value)

        if (expectedType is PsiArrayType && !isVararg) {
            return convertArrayInitializerValue(codeConverter,
                                                value.text,
                                                listOf({ _ -> expression }),
                                                expectedType,
                                                false
            ).assignPrototype(value)
        }
        return expression
    }

    private fun convertArrayInitializerValue(
            codeConverter: CodeConverter,
            valueText: String,
            componentGenerators: List<(CodeConverter) -> Expression>,
            expectedType: PsiType?,
            isVararg: Boolean
    ): Expression {
        val expectedTypeConverted = converter.typeConverter.convertType(expectedType)
        return if (expectedTypeConverted is ArrayType) {
            createArrayExpression(codeConverter, componentGenerators, expectedTypeConverted, isVararg)
        }
        else {
            DummyStringExpression(valueText)
        }
    }

    private fun createArrayExpression(codeConverter: CodeConverter, componentGenerators: List<(CodeConverter) -> Expression>, expectedType: ArrayType, isVararg: Boolean): Expression {
        val array = createArrayInitializerExpression(expectedType, componentGenerators.map { it(codeConverter) }, needExplicitType = false)
        return if (isVararg) {
            StarExpression(array.assignNoPrototype())
        }
        else {
            array
        }
    }
}
