/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringMessageDialog
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.refactoring.move.ContainerChangeInfo
import org.jetbrains.kotlin.idea.refactoring.move.ContainerInfo
import org.jetbrains.kotlin.idea.refactoring.move.postProcessMoveUsages
import org.jetbrains.kotlin.idea.refactoring.move.processInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

fun highlightElements(project: Project, editor: Editor?, elements: List<PsiElement>) {
    if (editor == null || ApplicationManager.getApplication().isUnitTestMode) return

    val editorColorsManager = EditorColorsManager.getInstance()
    val searchResultsAttributes = editorColorsManager.globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
    val highlightManager = HighlightManager.getInstance(project)
    highlightManager.addOccurrenceHighlights(editor, elements.toTypedArray(), searchResultsAttributes, true, null)
}

fun showDialog(
    project: Project,
    name: String,
    title: String,
    declaration: KtNamedDeclaration,
    usages: List<KtElement>,
    helpTopic: String? = null
): Boolean {
    if (ApplicationManager.getApplication().isUnitTestMode) return true

    val kind = when (declaration) {
        is KtProperty -> if (declaration.isLocal)
            KotlinBundle.message("text.local.variable")
        else
            KotlinBundle.message("text.local.property")
        is KtTypeAlias -> KotlinBundle.message("text.type.alias")
        else -> return false
    }
    val dialog = RefactoringMessageDialog(
        title,
        KotlinBundle.message("text.inline.0.1.2", kind, name, RefactoringBundle.message("occurences.string", usages.size)),
        helpTopic,
        "OptionPane.questionIcon",
        true,
        project
    )
    dialog.show()
    return dialog.isOK
}

internal var KtSimpleNameExpression.internalUsageInfos: MutableMap<FqName, (KtSimpleNameExpression) -> UsageInfo?>?
        by CopyablePsiUserDataProperty(Key.create("INTERNAL_USAGE_INFOS"))

internal fun preProcessInternalUsages(element: KtElement, usages: Collection<KtElement>) {
    val mainFile = element.containingKtFile
    val targetPackages = usages.mapTo(LinkedHashSet()) { it.containingKtFile.packageFqName }
    for (targetPackage in targetPackages) {
        if (targetPackage == mainFile.packageFqName) continue
        val packageNameInfo = ContainerChangeInfo(ContainerInfo.Package(mainFile.packageFqName), ContainerInfo.Package(targetPackage))
        element.processInternalReferencesToUpdateOnPackageNameChange(packageNameInfo) { expr, factory ->
            val infos = expr.internalUsageInfos ?: LinkedHashMap<FqName, (KtSimpleNameExpression) -> UsageInfo?>().apply {
                expr.internalUsageInfos = this
            }
            infos[targetPackage] = factory
        }
    }
}

internal fun <E : KtElement> postProcessInternalReferences(inlinedElement: E): E? {
    val pointer = inlinedElement.createSmartPointer()
    val targetPackage = inlinedElement.containingKtFile.packageFqName
    val expressionsToProcess = inlinedElement.collectDescendantsOfType<KtSimpleNameExpression> { it.internalUsageInfos != null }
    val internalUsages = expressionsToProcess.mapNotNull { it.internalUsageInfos!![targetPackage]?.invoke(it) }
    expressionsToProcess.forEach { it.internalUsageInfos = null }
    postProcessMoveUsages(internalUsages)
    return pointer.element
}

internal fun buildCodeToInline(
    declaration: KtDeclaration,
    returnType: KotlinType?,
    isReturnTypeExplicit: Boolean,
    bodyOrInitializer: KtExpression,
    isBlockBody: Boolean,
    editor: Editor?
): CodeToInline? {
    val bodyCopy = bodyOrInitializer.copied()

    val expectedType = if (!isBlockBody && isReturnTypeExplicit)
        returnType ?: TypeUtils.NO_EXPECTED_TYPE
    else
        TypeUtils.NO_EXPECTED_TYPE

    val scope by lazy { bodyOrInitializer.getResolutionScope() }
    fun analyzeExpressionInContext(expression: KtExpression): BindingContext = expression.analyzeInContext(
        scope = scope,
        contextExpression = bodyOrInitializer,
        expectedType = expectedType
    )

    val descriptor = declaration.unsafeResolveToDescriptor()
    val builder = CodeToInlineBuilder(descriptor as CallableDescriptor, declaration.getResolutionFacade())
    if (isBlockBody) {
        bodyCopy as KtBlockExpression
        val statements = bodyCopy.statements

        val returnStatements = bodyCopy.collectDescendantsOfType<KtReturnExpression> {
            val function = it.getStrictParentOfType<KtFunction>()
            if (function != null && function != declaration) return@collectDescendantsOfType false
            it.getLabelName().let { label -> label == null || label == declaration.name }
        }

        val lastReturn = statements.lastOrNull() as? KtReturnExpression
        if (returnStatements.any { it != lastReturn }) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                if (returnStatements.size > 1)
                    KotlinBundle.message("error.text.inline.function.is.not.supported.for.functions.with.multiple.return.statements")
                else
                    KotlinBundle.message("error.text.inline.function.is.not.supported.for.functions.with.return.statements.not.at.the.end.of.the.body")
            )

            CommonRefactoringUtil.showErrorHint(
                declaration.project,
                editor,
                message,
                KotlinBundle.message("title.inline.function"),
                null
            )

            return null
        }

        return builder.prepareCodeToInline(
            lastReturn?.returnedExpression,
            statements.dropLast(returnStatements.size), ::analyzeExpressionInContext, reformat = true
        )
    } else {
        return builder.prepareCodeToInline(bodyCopy, emptyList(), ::analyzeExpressionInContext, reformat = true)
    }
}

internal fun Editor.findSimpleNameReference(): KtSimpleNameReference? =
    when (val reference = TargetElementUtil.findReference(this, caretModel.offset)) {
        is KtSimpleNameReference -> reference
        is PsiMultiReference -> reference.references.firstIsInstanceOrNull()
        else -> null
    }
