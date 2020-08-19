/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.util.findDescendantOfType
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.unwrapSpecialUsageOrNull
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.findOrCreateUsageReplacementStrategy
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.findUsageReplacementStrategy
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import org.jetbrains.kotlin.j2k.JKMultipleFilesPostProcessingTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter.Companion.addImports
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.tower.isSynthesized

private val LOG = Logger.getInstance(JavaToKotlinInlineHandler::class.java)

class JavaToKotlinInlineHandler : AbstractCrossLanguageInlineHandler() {
    override fun prepareReference(reference: PsiReference, referenced: PsiElement): MultiMap<PsiElement, String> {
        val javaMemberToInline = referenced.javaMemberToInline ?: return super.prepareReference(reference, referenced)
        javaMemberToInline.validate()?.let { error ->
            return createMultiMapWithSingleConflict(
                reference.element,
                error,
            )
        }

        try {
            val strategy = findOrCreateUsageReplacementStrategy(javaMemberToInline, reference.element)
            if (strategy == null) KotlinBundle.message("failed.to.create.a.wrapper.for.inlining.to.kotlin") else null
        } catch (e: IllegalStateException) {
            LOG.error(e)
            e.message
        }?.let { errorMessage ->
            return createMultiMapWithSingleConflict(
                reference.element,
                errorMessage,
            )
        }

        return MultiMap.empty()
    }

    override fun performInline(usage: UsageInfo, referenced: PsiElement) {
        val unwrappedUsage = unwrapUsage(usage) ?: kotlin.run {
            LOG.error("Kotlin usage in $usage not found (element ${usage.element}")
            return
        }

        val unwrappedElement = unwrapElement(unwrappedUsage, referenced)
        val replacementStrategy = referenced.findUsageReplacementStrategy(withValidation = false) ?: kotlin.run {
            LOG.error("Can't find strategy for ${unwrappedElement.getKotlinFqName()} => ${unwrappedElement.text}")
            return
        }

        replacementStrategy.createReplacer(unwrappedElement)?.invoke()
    }
}

private val PsiElement.javaMemberToInline: PsiMember?
    get() = if (language == JavaLanguage.INSTANCE && (this is PsiMethod || this is PsiField)) this as PsiMember else null

private fun PsiMember.validate(): String? = when {
    this is PsiField && !this.hasInitializer() -> KotlinBundle.message("a.field.without.an.initializer.is.not.yet.supported")
    this is PsiMethod && this.isConstructor -> KotlinBundle.message("a.constructor.call.is.not.yet.supported")
    else -> null
}

private fun NewJavaToKotlinConverter.convertToKotlinNamedDeclaration(
    referenced: PsiMember,
    context: PsiElement,
): KtNamedDeclaration {
    val factory = KtPsiFactory(project)
    val className = referenced.containingClass?.qualifiedName
    val (j2kResults, _, j2kContext) = elementsToKotlin(listOf(referenced)) { it == referenced }
    val j2kResult = j2kResults.first() ?: error("Can't convert to Kotlin ${referenced.text}")
    val fakeFile = factory.createAnalyzableFile("dummy.kt", "class DuMmY_42_ : $className {\n${j2kResult.text}\n}", context).also {
        it.addImports(j2kResult.importsToAdd)
    }

    J2kConverterExtension.extension(useNewJ2k = true).createPostProcessor(formatCode = true).doAdditionalProcessing(
        JKMultipleFilesPostProcessingTarget(listOf(fakeFile)),
        j2kContext
    ) { _, _ -> }

    return fakeFile.findDescendantOfType {
        it.name == referenced.name
    } ?: error("Can't find ${referenced.name} declaration in ${fakeFile.text}")
}

private fun unwrapUsage(usage: UsageInfo): KtReferenceExpression? {
    val ktReferenceExpression = usage.element as? KtReferenceExpression ?: return null
    return unwrapSpecialUsageOrNull(ktReferenceExpression) ?: ktReferenceExpression
}

private fun unwrapElement(unwrappedUsage: KtReferenceExpression, referenced: PsiElement): KtReferenceExpression {
    if (referenced !is PsiMember) return unwrappedUsage
    val name = referenced.name ?: return unwrappedUsage
    if (unwrappedUsage.textMatches(name)) return unwrappedUsage

    val qualifiedElementOrReference = unwrappedUsage.getQualifiedExpressionForSelectorOrThis()
    val assignment = qualifiedElementOrReference.getAssignmentByLHS()?.takeIf { it.operationToken == KtTokens.EQ } ?: return unwrappedUsage
    val argument = assignment.right ?: return unwrappedUsage
    if (unwrappedUsage.resolveToCall()?.resultingDescriptor?.isSynthesized != true) return unwrappedUsage

    val psiFactory = KtPsiFactory(unwrappedUsage)
    val callExpression = psiFactory.createExpressionByPattern("$name($0)", argument) as? KtCallExpression ?: return unwrappedUsage
    val resultExpression = assignment.replaced(unwrappedUsage.replaced(callExpression).getQualifiedExpressionForSelectorOrThis())
    return resultExpression.getQualifiedElementSelector() as KtReferenceExpression
}

internal class J2KInlineCache(private val strategy: UsageReplacementStrategy, private val originalText: String) {
    /**
     * @return [strategy] without validation if [elementToValidation] is null
     */
    private fun getStrategy(elementToValidation: PsiElement?): UsageReplacementStrategy? = strategy.takeIf {
        elementToValidation?.textMatches(originalText) != false
    }

    companion object {
        private val JAVA_TO_KOTLIN_INLINE_CACHE_KEY = Key<J2KInlineCache>("JAVA_TO_KOTLIN_INLINE_CACHE")

        internal fun PsiElement.findUsageReplacementStrategy(withValidation: Boolean): UsageReplacementStrategy? =
            getUserData(JAVA_TO_KOTLIN_INLINE_CACHE_KEY)?.getStrategy(this.takeIf { withValidation })

        internal fun PsiElement.setUsageReplacementStrategy(strategy: UsageReplacementStrategy): Unit =
            putUserData(JAVA_TO_KOTLIN_INLINE_CACHE_KEY, J2KInlineCache(strategy, text))

        internal fun findOrCreateUsageReplacementStrategy(javaMember: PsiMember, context: PsiElement): UsageReplacementStrategy? {
            javaMember.findUsageReplacementStrategy(withValidation = true)?.let { return it }

            val converter = NewJavaToKotlinConverter(
                javaMember.project,
                javaMember.module,
                ConverterSettings.defaultSettings,
                IdeaJavaToKotlinServices
            )

            val declaration = converter.convertToKotlinNamedDeclaration(
                referenced = javaMember,
                context = context,
            )

            return createUsageReplacementStrategyForNamedDeclaration(
                declaration,
                javaMember.findExistingEditor()
            )?.also { javaMember.setUsageReplacementStrategy(it) }
        }
    }
}

private fun createUsageReplacementStrategyForNamedDeclaration(
    namedDeclaration: KtNamedDeclaration,
    editor: Editor?
): UsageReplacementStrategy? = when (namedDeclaration) {
    is KtNamedFunction -> createUsageReplacementStrategyForFunction(namedDeclaration, editor)
    is KtProperty -> createReplacementStrategyForProperty(namedDeclaration, editor, namedDeclaration.project)
    else -> null
}
