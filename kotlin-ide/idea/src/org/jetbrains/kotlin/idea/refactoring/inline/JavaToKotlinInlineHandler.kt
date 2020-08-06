/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.findDescendantOfType
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.unwrapSpecialUsageOrNull
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.findOrCreateUsageReplacementStrategy
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.findUsageReplacementStrategy
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import org.jetbrains.kotlin.j2k.JKMultipleFilesPostProcessingTarget
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter.Companion.addImports
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression

private val LOG = Logger.getInstance(JavaToKotlinInlineHandler::class.java)

class JavaToKotlinInlineHandler : AbstractCrossLanguageInlineHandler() {
    override fun prepareReference(reference: PsiReference, referenced: PsiElement): MultiMap<PsiElement, String> {
        if (referenced.language != JavaLanguage.INSTANCE || referenced !is PsiMethod) return super.prepareReference(reference, referenced)
        try {
            val strategy = findOrCreateUsageReplacementStrategy(referenced, reference.element)
            "Failed to create usage strategy".takeIf { strategy == null }
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
        val unwrappedElement = unwrapUsage(usage) ?: run {
            LOG.error("Kotlin usage in $usage not found (element ${usage.element}")
            return
        }

        val replacementStrategy = referenced.findUsageReplacementStrategy(withValidation = false) ?: run {
            LOG.error("Can't find strategy for ${unwrappedElement.getKotlinFqName()} => ${unwrappedElement.text}")
            return
        }

        replacementStrategy.createReplacer(unwrappedElement)?.invoke()
    }
}

private fun NewJavaToKotlinConverter.convertToKotlinAndFindFunction(
    referenced: PsiMethod,
    context: PsiElement,
): KtNamedFunction {
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
    } ?: error("Can't find ${referenced.name} function in ${fakeFile.text}")
}

private fun unwrapUsage(usage: UsageInfo): KtReferenceExpression? {
    val ktReferenceExpression = usage.element as? KtReferenceExpression ?: return null
    return unwrapSpecialUsageOrNull(ktReferenceExpression) ?: ktReferenceExpression
}

internal class J2KInlineCache(private val strategy: UsageReplacementStrategy, private val originalText: String) {
    /**
     * @return [strategy] without validation if [elementToValidation] is null
     */
    fun getStrategy(elementToValidation: PsiElement?): UsageReplacementStrategy? = strategy.takeIf {
        elementToValidation?.textMatches(originalText) != false
    }

    companion object {
        private val JAVA_TO_KOTLIN_INLINE_CACHE_KEY = Key<J2KInlineCache>("JAVA_TO_KOTLIN_INLINE_CACHE")

        internal fun PsiElement.findUsageReplacementStrategy(withValidation: Boolean): UsageReplacementStrategy? =
            getUserData(JAVA_TO_KOTLIN_INLINE_CACHE_KEY)?.getStrategy(this.takeIf { withValidation })

        internal fun PsiElement.setUsageReplacementStrategy(strategy: UsageReplacementStrategy): Unit =
            putUserData(JAVA_TO_KOTLIN_INLINE_CACHE_KEY, J2KInlineCache(strategy, text))

        internal fun findOrCreateUsageReplacementStrategy(javaMethod: PsiMethod, context: PsiElement): UsageReplacementStrategy? {
            javaMethod.findUsageReplacementStrategy(withValidation = true)?.let { return it }

            val converter = NewJavaToKotlinConverter(
                javaMethod.project,
                javaMethod.module,
                ConverterSettings.defaultSettings,
                IdeaJavaToKotlinServices
            )

            val declaration = converter.convertToKotlinAndFindFunction(
                referenced = javaMethod,
                context = context,
            )

            return createUsageReplacementStrategy(
                declaration,
                javaMethod.findExistingEditor()
            )?.also { javaMethod.setUsageReplacementStrategy(it) }
        }
    }
}