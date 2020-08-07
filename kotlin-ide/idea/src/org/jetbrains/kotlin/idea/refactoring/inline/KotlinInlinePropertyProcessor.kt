/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.Nls
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
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

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
    override fun createReplacementStrategy(declaration: KtProperty, editor: Editor?): UsageReplacementStrategy? {
        val getter = declaration.getter?.takeIf { it.hasBody() }
        val setter = declaration.setter?.takeIf { it.hasBody() }

        val readReplacement: CodeToInline?
        val writeReplacement: CodeToInline?
        val descriptor = declaration.unsafeResolveToDescriptor() as ValueDescriptor
        val isTypeExplicit = declaration.typeReference != null
        if (getter == null && setter == null) {
            val (referenceExpressions, _) = findUsages(declaration)
            val initialization = extractInitialization(declaration, referenceExpressions, myProject, editor) ?: return null
            readReplacement = buildCodeToInline(declaration, descriptor.type, isTypeExplicit, initialization.value, false, editor) ?: return null
            writeReplacement = null
        } else {
            readReplacement = getter?.let {
                buildCodeToInline(getter, descriptor.type, isTypeExplicit, getter.bodyExpression!!, getter.hasBlockBody(), editor) ?: return null
            }
            writeReplacement = setter?.let {
                buildCodeToInline(setter, setter.builtIns.unitType, true, setter.bodyExpression!!, setter.hasBlockBody(), editor) ?: return null
            }
        }

        return PropertyUsageReplacementStrategy(readReplacement, writeReplacement)
    }

    override fun postAction(declaration: KtProperty) {
        if (deleteAfter && isWhenSubjectVariable) {
            declaration.initializer?.let { declaration.replace(it) }
        }
    }

    override fun postDeleteAction() {
        statementToDelete?.delete()
    }

    companion object {
        fun findUsages(declaration: KtProperty): Usages {
            val references = ReferencesSearchScopeHelper.search(declaration)
            val referenceExpressions = mutableListOf<KtExpression>()
            val conflictUsages = MultiMap.create<PsiElement, String>()
            for (ref in references) {
                val refElement = ref.element
                if (refElement !is KtElement) {
                    conflictUsages.putValue(refElement, KotlinBundle.message("non.kotlin.usage.0", refElement.text))
                    continue
                }

                val expression = (refElement as? KtExpression)?.getQualifiedExpressionForSelectorOrThis()
                //TODO: what if null?
                if (expression != null) {
                    if (expression.readWriteAccess(useResolveForReadWrite = true) == ReferenceAccess.READ_WRITE) {
                        conflictUsages.putValue(expression, KotlinBundle.message("unsupported.usage.0", expression.parent.text))
                    }
                    referenceExpressions.add(expression)
                }
            }
            return Usages(referenceExpressions, conflictUsages)
        }

        data class Usages(val referenceExpressions: Collection<KtExpression>, val conflicts: MultiMap<PsiElement, String>)

        data class Initialization(val value: KtExpression, val assignment: KtBinaryExpression?)

        fun extractInitialization(
            declaration: KtProperty,
            referenceExpressions: Collection<KtExpression>,
            project: Project,
            editor: Editor?
        ): Initialization? {
            val writeUsages = referenceExpressions.filter { it.readWriteAccess(useResolveForReadWrite = true) != ReferenceAccess.READ }

            val initializerInDeclaration = declaration.initializer
            if (initializerInDeclaration != null) {
                if (writeUsages.isNotEmpty()) {
                    reportAmbiguousAssignment(project, editor, declaration.name!!, writeUsages)
                    return null
                }
                return Initialization(initializerInDeclaration, assignment = null)
            } else {
                val assignment = writeUsages.singleOrNull()
                    ?.getAssignmentByLHS()
                    ?.takeIf { it.operationToken == KtTokens.EQ }
                val initializer = assignment?.right
                if (initializer == null) {
                    reportAmbiguousAssignment(project, editor, declaration.name!!, writeUsages)
                    return null
                }
                return Initialization(initializer, assignment)
            }
        }

        fun reportAmbiguousAssignment(project: Project, editor: Editor?, name: String, assignments: Collection<PsiElement>) {
            val key = if (assignments.isEmpty()) "variable.has.no.initializer" else "variable.has.no.dominating.definition"
            val message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name))
            showErrorHint(project, editor, message)
        }

        fun showErrorHint(project: Project, editor: Editor?, @Nls message: String) {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                message,
                RefactoringBundle.message("inline.variable.title"),
                HelpID.INLINE_VARIABLE
            )
        }
    }
}

