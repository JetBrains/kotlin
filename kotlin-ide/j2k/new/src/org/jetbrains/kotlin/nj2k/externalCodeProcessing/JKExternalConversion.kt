/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.externalCodeProcessing

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.j2k.usageProcessing.AccessorToPropertyProcessing
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal sealed class JKExternalConversion : Comparable<JKExternalConversion> {
    abstract val usage: PsiElement

    abstract fun apply()

    private val depth by lazy(LazyThreadSafetyMode.NONE) { usage.parentsWithSelf.takeWhile { it !is PsiFile }.count() }
    private val offset by lazy(LazyThreadSafetyMode.NONE) { usage.textRange.startOffset }

    override fun compareTo(other: JKExternalConversion): Int {
        val depth1 = depth
        val depth2 = other.depth
        if (depth1 != depth2) { // put deeper elements first to not invalidate them when processing ancestors
            return -depth1.compareTo(depth2)
        }

        // process elements with the same deepness from right to left
        // so that right-side of assignments is not invalidated by processing of the left one
        return -offset.compareTo(other.offset)
    }
}

internal class AccessorToPropertyKotlinExternalConversion(
    private val name: String,
    private val accessorKind: AccessorKind,
    override val usage: PsiElement
) : JKExternalConversion() {
    override fun apply() {
        AccessorToPropertyProcessing.processUsage(usage, name, accessorKind)
    }
}

internal class AccessorToPropertyJavaExternalConversion(
    private val name: String,
    private val accessorKind: AccessorKind,
    override val usage: PsiElement
) : JKExternalConversion() {
    override fun apply() {
        if (usage !is PsiReferenceExpression) return
        val methodCall = usage.parent as? PsiMethodCallExpression ?: return

        val factory = PsiElementFactory.getInstance(usage.project)
        val propertyAccess = factory.createReferenceExpression(usage.qualifierExpression)
        val newExpression = when (accessorKind) {
            AccessorKind.GETTER -> propertyAccess
            AccessorKind.SETTER -> {
                val value = methodCall.argumentList.expressions.singleOrNull() ?: return
                factory.createAssignment(propertyAccess, value)
            }
        }
        methodCall.replace(newExpression)
    }

    private fun PsiElementFactory.createReferenceExpression(qualifier: PsiExpression?): PsiReferenceExpression =
        createExpressionFromText(qualifier?.let { "qualifier." }.orEmpty() + name, usage).cast<PsiReferenceExpression>().apply {
            qualifierExpression?.replace(qualifier ?: return@apply)
        }

    private fun PsiElementFactory.createAssignment(target: PsiExpression, value: PsiExpression): PsiAssignmentExpression =
        createExpressionFromText("x = 1", usage).cast<PsiAssignmentExpression>().apply {
            lExpression.replace(target)
            rExpression!!.replace(value)
        }
}

internal class PropertyRenamedKotlinExternalUsageConversion(
    private val newName: String,
    override val usage: KtElement
) : JKExternalConversion() {
    override fun apply() {
        if (usage !is KtSimpleNameExpression) return
        val factory = KtPsiFactory(usage)
        usage.getReferencedNameElement().replace(factory.createExpression(newName))
    }
}

internal class PropertyRenamedJavaExternalUsageConversion(
    private val newName: String,
    override val usage: PsiElement
) : JKExternalConversion() {
    override fun apply() {
        if (usage !is PsiReferenceExpression) return
        val factory = PsiElementFactory.getInstance(usage.project)
        usage.referenceNameElement?.replace(factory.createExpressionFromText(newName, usage))
    }
}
