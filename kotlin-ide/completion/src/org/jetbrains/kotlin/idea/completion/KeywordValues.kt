/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.completion.smart.ExpectedInfoMatch
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import org.jetbrains.kotlin.idea.completion.smart.matchExpectedInfo
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

object KeywordValues {
    interface Consumer {
        /**
         * @param suitableOnPsiLevel together with [expectedInfoMatcher] the function is called to make a decision whether
         * [factory's][factory] output should be tagged with [priority]. Note that the function is used only as a fallback (in the case
         * where [expectedInfoMatcher] has no input data to process).
         *
         * Function receiver is a current position of the caret.
         */
        fun consume(
            lookupString: String,
            expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
            suitableOnPsiLevel: PsiElement.() -> Boolean = { false },
            priority: SmartCompletionItemPriority,
            factory: () -> LookupElement
        )
    }

    fun process(
        consumer: Consumer,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
        moduleDescriptor: ModuleDescriptor,
        isJvmModule: Boolean
    ) {
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val booleanInfoMatcher = matcher@{ info: ExpectedInfo ->
                // no sense in true or false as if-condition or when entry for when with no subject
                val additionalData = info.additionalData
                val skipTrueFalse = when (additionalData) {
                    is IfConditionAdditionalData -> true
                    is WhenEntryAdditionalData -> !additionalData.whenWithSubject
                    else -> false
                }
                if (skipTrueFalse) {
                    return@matcher ExpectedInfoMatch.noMatch
                }

                if (info.fuzzyType?.type?.isBooleanOrNullableBoolean() ?: false)
                    ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)
                else
                    ExpectedInfoMatch.noMatch
            }
            consumer.consume("true", booleanInfoMatcher, priority =  SmartCompletionItemPriority.TRUE) {
                LookupElementBuilder.create(KeywordLookupObject(), "true").bold()
            }
            consumer.consume("false", booleanInfoMatcher, priority = SmartCompletionItemPriority.FALSE) {
                LookupElementBuilder.create(KeywordLookupObject(), "false").bold()
            }

            val nullMatcher = { info: ExpectedInfo ->
                when {
                    (info.additionalData as? ComparisonOperandAdditionalData)?.suppressNullLiteral == true -> ExpectedInfoMatch.noMatch

                    info.fuzzyType?.type?.isMarkedNullable == true -> ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)

                    else -> ExpectedInfoMatch.noMatch
                }
            }

            // Position (this) is suitable for 'null' when the caret points to a function argument (fun is not imported!) or
            // it's a part of a binary expression (left- or right- hand side) with unknown declarations.
            val positionIsSuitableForNull: PsiElement.() -> Boolean = {
                elementType == KtTokens.IDENTIFIER
                        && (context?.parent is KtValueArgument || (context is KtExpression
                        && ((context as KtExpression).getPrevSiblingIgnoringWhitespaceAndComments() is KtOperationReferenceExpression
                        || (context as KtExpression).getNextSiblingIgnoringWhitespaceAndComments() is KtOperationReferenceExpression)))
            }

            consumer.consume("null", nullMatcher, positionIsSuitableForNull, SmartCompletionItemPriority.NULL) {
                LookupElementBuilder.create(KeywordLookupObject(), "null").bold()
            }
        }

        if (callTypeAndReceiver is CallTypeAndReceiver.CALLABLE_REFERENCE && callTypeAndReceiver.receiver != null) {
            val qualifierType = bindingContext.get(BindingContext.DOUBLE_COLON_LHS, callTypeAndReceiver.receiver!!)?.type
            if (qualifierType != null) {

                @OptIn(FrontendInternals::class)
                val kClassDescriptor = resolutionFacade.getFrontendService(ReflectionTypes::class.java).kClass
                val classLiteralType =
                    KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClassDescriptor, listOf(TypeProjectionImpl(qualifierType)))
                val kClassTypes = listOf(classLiteralType.toFuzzyType(emptyList()))
                val kClassMatcher = { info: ExpectedInfo -> kClassTypes.matchExpectedInfo(info) }
                consumer.consume("class", kClassMatcher, priority =  SmartCompletionItemPriority.CLASS_LITERAL) {
                    LookupElementBuilder.create(KeywordLookupObject(), "class").bold()
                }

                if (isJvmModule) {
                    val javaLangClassDescriptor = resolutionFacade.resolveImportReference(moduleDescriptor, FqName("java.lang.Class"))
                        .singleOrNull() as? ClassDescriptor

                    if (javaLangClassDescriptor != null) {
                        val javaLangClassType = KotlinTypeFactory.simpleNotNullType(
                            Annotations.EMPTY,
                            javaLangClassDescriptor,
                            listOf(TypeProjectionImpl(qualifierType))
                        )
                        val javaClassTypes = listOf(javaLangClassType.toFuzzyType(emptyList()))
                        val javaClassMatcher = { info: ExpectedInfo -> javaClassTypes.matchExpectedInfo(info) }
                        consumer.consume("class", javaClassMatcher, priority = SmartCompletionItemPriority.CLASS_LITERAL) {
                            LookupElementBuilder.create(KeywordLookupObject(), "class.java")
                                .withPresentableText("class")
                                .withTailText(".java")
                                .bold()
                        }
                    }
                }
            }
        }
    }
}
