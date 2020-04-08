/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ReceiverType
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi

class DslMembersCompletion(
    private val prefixMatcher: PrefixMatcher,
    private val elementFactory: LookupElementFactory,
    receiverTypes: List<ReceiverType>?,
    private val collector: LookupElementsCollector,
    private val indicesHelper: KotlinIndicesHelper,
    private val callTypeAndReceiver: CallTypeAndReceiver<*, *>
) {
    /**
     * It is stated that `two implicit receivers of the same DSL are not accessible in the same scope`,
     * that's why we need only the last one of the receivers to provide the completion (see [DslMarker]).
     *
     * When the last receiver is not a part of DSL, [nearestReceiverMarkers] will be empty, and dsl
     * members would not be suggested in the autocompletion (see KT-30996).
     */
    private val nearestReceiver = receiverTypes?.lastOrNull()
    private val nearestReceiverMarkers = nearestReceiver?.takeIf { it.implicit }?.extractDslMarkers().orEmpty()

    fun completeDslFunctions() {
        if (nearestReceiver == null || nearestReceiverMarkers.isEmpty()) return

        val receiverMarkersShortNames = nearestReceiverMarkers.map { it.shortName() }.distinct()
        val extensionDescriptors = indicesHelper.getCallableTopLevelExtensions(
            nameFilter = { prefixMatcher.prefixMatches(it) },
            declarationFilter = {
                (it as KtModifierListOwner).modifierList?.collectAnnotationEntriesFromStubOrPsi()
                    ?.any { it.shortName in receiverMarkersShortNames }
                    ?: false
            },
            callTypeAndReceiver = callTypeAndReceiver,
            receiverTypes = listOf(nearestReceiver.type)
        )
        extensionDescriptors.forEach {
            collector.addDescriptorElements(it, elementFactory, notImported = true, withReceiverCast = false, prohibitDuplicates = true)
        }

        collector.flushToResultSet()
    }

}
