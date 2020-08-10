/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.PropertyUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinInlinePropertyProcessor(
    declaration: KtProperty,
    reference: KtSimpleNameReference?,
    inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    private val isWhenSubjectVariable: Boolean,
    editor: Editor?,
    private val statementToDelete: KtBinaryExpression?,
) : AbstractKotlinInlineDeclarationProcessor<KtProperty>(
    declaration = declaration,
    reference = reference,
    inlineThisOnly = inlineThisOnly,
    deleteAfter = deleteAfter && !isWhenSubjectVariable,
    editor = editor,
) {
    override fun createReplacementStrategy(): UsageReplacementStrategy? {
        return createReplacementStrategyForProperty(declaration, editor, myProject)
    }

    override fun postAction() {
        if (deleteAfter && isWhenSubjectVariable) {
            declaration.initializer?.let { declaration.replace(it) }
        }
    }

    override fun postDeleteAction() {
        statementToDelete?.delete()
    }

    override fun additionalPreprocessUsages(usages: Array<out UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        val initializer by lazy { extractInitialization(declaration, myProject, editor, withErrorMessage = false)?.assignment?.left }
        for (usage in usages) {
            val expression = usage.element?.writeOrReadWriteExpression ?: continue
            if (expression != initializer) {
                conflicts.putValue(expression, KotlinBundle.message("unsupported.usage.0", expression.parent.text))
            }
        }
    }

    companion object {
        class Initialization(val value: KtExpression, val assignment: KtBinaryExpression?)

        fun extractInitialization(
            property: KtProperty,
            project: Project,
            editor: Editor?,
            withErrorMessage: Boolean = true,
        ): Initialization? {
            val definitionScope = property.parent ?: kotlin.run {
                if (withErrorMessage) reportAmbiguousAssignment(project, editor, property.name!!, emptyList())
                return null
            }

            val writeUsages = mutableListOf<KtExpression>()
            ReferencesSearchScopeHelper.search(property, LocalSearchScope(definitionScope)).forEach {
                val expression = it.element.writeOrReadWriteExpression ?: return@forEach
                writeUsages += expression
            }

            val initializerInDeclaration = property.initializer
            if (initializerInDeclaration != null) {
                if (writeUsages.isNotEmpty()) {
                    if (withErrorMessage) reportAmbiguousAssignment(project, editor, property.name!!, writeUsages)
                    return null
                }

                return Initialization(initializerInDeclaration, assignment = null)
            }

            val assignment = writeUsages.singleOrNull()?.getAssignmentByLHS()?.takeIf { it.operationToken == KtTokens.EQ }
            val initializer = assignment?.right
            if (initializer == null) {
                if (withErrorMessage) reportAmbiguousAssignment(project, editor, property.name!!, writeUsages)
                return null
            }

            return Initialization(initializer, assignment)
        }
    }
}

fun createReplacementStrategyForProperty(property: KtProperty, editor: Editor?, project: Project): UsageReplacementStrategy? {
    val getter = property.getter?.takeIf { it.hasBody() }
    val setter = property.setter?.takeIf { it.hasBody() }

    val readReplacement: CodeToInline?
    val writeReplacement: CodeToInline?
    val descriptor = property.unsafeResolveToDescriptor() as ValueDescriptor
    val isTypeExplicit = property.typeReference != null
    if (getter == null && setter == null) {
        val initialization = KotlinInlinePropertyProcessor.extractInitialization(property, project, editor) ?: return null
        readReplacement = buildCodeToInline(
            declaration = property,
            returnType = descriptor.type,
            isReturnTypeExplicit = isTypeExplicit,
            bodyOrInitializer = initialization.value,
            isBlockBody = false,
            editor = editor
        ) ?: return null

        writeReplacement = null
    } else {
        readReplacement = getter?.let {
            buildCodeToInline(
                declaration = getter,
                returnType = descriptor.type,
                isReturnTypeExplicit = isTypeExplicit,
                bodyOrInitializer = getter.bodyExpression!!,
                isBlockBody = getter.hasBlockBody(),
                editor = editor
            ) ?: return null
        }
        writeReplacement = setter?.let {
            buildCodeToInline(
                declaration = setter,
                returnType = setter.builtIns.unitType,
                isReturnTypeExplicit = true,
                bodyOrInitializer = setter.bodyExpression!!,
                isBlockBody = setter.hasBlockBody(),
                editor = editor
            ) ?: return null
        }
    }

    return PropertyUsageReplacementStrategy(readReplacement, writeReplacement)
}

private fun reportAmbiguousAssignment(project: Project, editor: Editor?, name: String, assignments: Collection<PsiElement>) {
    val key = if (assignments.isEmpty()) "variable.has.no.initializer" else "variable.has.no.dominating.definition"
    val message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name))
    KotlinInlineValHandler.showErrorHint(project, editor, message)
}

private val PsiElement.writeOrReadWriteExpression: KtExpression?
    get() = this.safeAs<KtExpression>()
        ?.getQualifiedExpressionForSelectorOrThis()
        ?.takeIf { it.readWriteAccess(useResolveForReadWrite = true) != ReferenceAccess.READ }
