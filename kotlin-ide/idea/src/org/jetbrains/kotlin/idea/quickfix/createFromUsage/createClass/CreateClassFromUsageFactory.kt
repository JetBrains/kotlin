/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.psi.KtElement

abstract class CreateClassFromUsageFactory<E : KtElement> : KotlinIntentionActionFactoryWithDelegate<E, ClassInfo>() {
    protected abstract fun getPossibleClassKinds(element: E, diagnostic: Diagnostic): List<ClassKind>

    override fun createFixes(
        originalElementPointer: SmartPsiElementPointer<E>,
        diagnostic: Diagnostic,
        quickFixDataFactory: () -> ClassInfo?
    ): List<QuickFixWithDelegateFactory> {
        val possibleClassKinds = getPossibleClassKinds(originalElementPointer.element ?: return emptyList(), diagnostic)

        return possibleClassKinds.map { classKind ->
            QuickFixWithDelegateFactory(classKind.actionPriority) {
                val currentElement = originalElementPointer.element ?: return@QuickFixWithDelegateFactory null
                val data = quickFixDataFactory() ?: return@QuickFixWithDelegateFactory null
                CreateClassFromUsageFix.create(currentElement, data.copy(kind = classKind))
            }
        }
    }
}