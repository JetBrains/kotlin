/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.PropertyUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
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
    reference: PsiReference?,
    inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    private val isWhenSubjectVariable: Boolean,
    editor: Editor?,
    private val statementToDelete: KtBinaryExpression?,
    project: Project,
) : AbstractKotlinInlineNamedDeclarationProcessor<KtProperty>(
    declaration = declaration,
    reference = reference,
    inlineThisOnly = inlineThisOnly,
    deleteAfter = deleteAfter && !isWhenSubjectVariable,
    editor = editor,
    project = project,
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
        val initializer by lazy { extractInitialization(declaration).initializerOrNull?.assignment?.left }
        for (usage in usages) {
            val expression = usage.element?.writeOrReadWriteExpression ?: continue
            if (expression != initializer) {
                conflicts.putValue(expression, KotlinBundle.message("unsupported.usage.0", expression.parent.text))
            }
        }
    }

    class Initializer(val value: KtExpression, val assignment: KtBinaryExpression?)

    class Initialization private constructor(
        private val value: KtExpression?,
        private val assignment: KtBinaryExpression?,
        @Nls private val error: String,
    ) {
        val initializerOrNull: Initializer?
            get() = value?.let { Initializer(value, assignment) }

        fun getInitializerOrShowErrorHint(project: Project, editor: Editor?): Initializer? {
            val initializer = initializerOrNull
            if (initializer != null) return initializer

            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                error,
                KotlinBundle.message("title.inline.property"),
                HelpID.INLINE_VARIABLE,
            )
            return null
        }

        companion object {
            fun createError(@Nls error: String): Initialization = Initialization(
                value = null,
                assignment = null,
                error = error,
            )

            fun createInitializer(value: KtExpression, assignment: KtBinaryExpression?): Initialization = Initialization(
                value = value,
                assignment = assignment,
                error = "",
            )
        }
    }

    companion object {
        fun extractInitialization(property: KtProperty): Initialization {
            val definitionScope = property.parent ?: kotlin.run {
                return createInitializationWithError(property.name!!, emptyList())
            }

            val writeUsages = mutableListOf<KtExpression>()
            ReferencesSearchScopeHelper.search(property, LocalSearchScope(definitionScope)).forEach {
                val expression = it.element.writeOrReadWriteExpression ?: return@forEach
                writeUsages += expression
            }

            val initializerInDeclaration = property.initializer
            if (initializerInDeclaration != null) {
                if (writeUsages.isNotEmpty()) {
                    return createInitializationWithError(property.name!!, writeUsages)
                }

                return Initialization.createInitializer(initializerInDeclaration, assignment = null)
            }

            val assignment = writeUsages.singleOrNull()?.getAssignmentByLHS()?.takeIf { it.operationToken == KtTokens.EQ }
            val initializer = assignment?.right
            if (initializer == null) {
                return createInitializationWithError(property.name!!, writeUsages)
            }

            return Initialization.createInitializer(initializer, assignment)
        }

        private fun createInitializationWithError(name: String, assignments: Collection<PsiElement>): Initialization {
            val key = if (assignments.isEmpty()) "variable.has.no.initializer" else "variable.has.no.dominating.definition"
            val message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name))
            return Initialization.createError(message)
        }
    }
}

fun createReplacementStrategyForProperty(property: KtProperty, editor: Editor?, project: Project): UsageReplacementStrategy? {
    val getter = property.getter?.takeIf { it.hasBody() }
    val setter = property.setter?.takeIf { it.hasBody() }

    val readReplacement: CodeToInline?
    val writeReplacement: CodeToInline?
    if (getter == null && setter == null) {
        val value = KotlinInlinePropertyProcessor.extractInitialization(property).getInitializerOrShowErrorHint(project, editor)?.value
            ?: return null

        readReplacement = buildCodeToInline(
            declaration = property,
            bodyOrInitializer = value,
            isBlockBody = false,
            editor = editor
        ) ?: return null

        writeReplacement = null
    } else {
        readReplacement = getter?.let {
            buildCodeToInline(
                declaration = getter,
                bodyOrInitializer = getter.bodyExpression!!,
                isBlockBody = getter.hasBlockBody(),
                editor = editor
            ) ?: return null
        }

        writeReplacement = setter?.let {
            buildCodeToInline(
                declaration = setter,
                bodyOrInitializer = setter.bodyExpression!!,
                isBlockBody = setter.hasBlockBody(),
                editor = editor
            ) ?: return null
        }
    }

    return PropertyUsageReplacementStrategy(readReplacement, writeReplacement)
}

private val PsiElement.writeOrReadWriteExpression: KtExpression?
    get() = this.safeAs<KtExpression>()
        ?.getQualifiedExpressionForSelectorOrThis()
        ?.takeIf { it.readWriteAccess(useResolveForReadWrite = true) != ReferenceAccess.READ }
