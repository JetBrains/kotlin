/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ForLoopConventionsChecker
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.util.*

class IterableTypesDetection(
    private val project: Project,
    private val forLoopConventionsChecker: ForLoopConventionsChecker,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    companion object {
        private val iteratorName = Name.identifier("iterator")
    }

    fun createDetector(scope: LexicalScope): IterableTypesDetector {
        return Detector(scope)
    }

    private inner class Detector(private val scope: LexicalScope) : IterableTypesDetector {
        private val cache = HashMap<FuzzyType, FuzzyType?>()

        private val typesWithExtensionIterator: Collection<KotlinType> = scope
            .collectFunctions(iteratorName, NoLookupLocation.FROM_IDE)
            .filter { it.isValidOperator() }
            .mapNotNull { it.extensionReceiverParameter?.type }

        override fun isIterable(type: FuzzyType, loopVarType: KotlinType?): Boolean {
            val elementType = elementType(type) ?: return false
            return loopVarType == null || elementType.checkIsSubtypeOf(loopVarType) != null
        }

        override fun isIterable(type: KotlinType, loopVarType: KotlinType?): Boolean =
            isIterable(type.toFuzzyType(emptyList()), loopVarType)

        private fun elementType(type: FuzzyType): FuzzyType? {
            return cache.getOrPutNullable(type, { elementTypeNoCache(type) })
        }

        override fun elementType(type: KotlinType): FuzzyType? = elementType(type.toFuzzyType(emptyList()))

        private fun elementTypeNoCache(type: FuzzyType): FuzzyType? {
            // optimization
            if (!canBeIterable(type)) return null

            val expression = KtPsiFactory(project).createExpression("fake")
            val context = ExpressionTypingContext.newContext(
                BindingTraceContext(), scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, languageVersionSettings, dataFlowValueFactory
            )
            val expressionReceiver = ExpressionReceiver.create(expression, type.type, context.trace.bindingContext)
            val elementType = forLoopConventionsChecker.checkIterableConvention(expressionReceiver, context)
            return elementType?.let { it.toFuzzyType(type.freeParameters) }
        }

        private fun canBeIterable(type: FuzzyType): Boolean {
            if (type.type.constructor is IntegerLiteralTypeConstructor) return false
            return type.type.memberScope.getContributedFunctions(iteratorName, NoLookupLocation.FROM_IDE).isNotEmpty() ||
                    typesWithExtensionIterator.any {
                        val freeParams = it.arguments.mapNotNull { it.type.constructor.declarationDescriptor as? TypeParameterDescriptor }
                        type.checkIsSubtypeOf(it.toFuzzyType(freeParams)) != null
                    }
        }
    }
}

interface IterableTypesDetector {
    fun isIterable(type: KotlinType, loopVarType: KotlinType? = null): Boolean

    fun isIterable(type: FuzzyType, loopVarType: KotlinType? = null): Boolean

    fun elementType(type: KotlinType): FuzzyType?
}