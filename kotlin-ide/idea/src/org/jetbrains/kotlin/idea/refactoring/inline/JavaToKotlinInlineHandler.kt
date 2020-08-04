/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findDescendantOfType
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.unwrapSpecialUsageOrNull
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

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

        val replacementStrategy = referenced.findUsageReplacementStrategy() ?: run {
            LOG.error("Can't find strategy for ${unwrappedElement.getKotlinFqName()} => ${unwrappedElement.text}")
            return
        }

        replacementStrategy.createReplacer(unwrappedElement)?.invoke()
    }
}

private fun KtFile.patchFakeClass(factory: KtPsiFactory): KtFile {
    val resultClass = this.declarations.firstOrNull() as? KtClassOrObject
    val className = resultClass?.name ?: throw KotlinExceptionWithAttachments("class without name")
        .withAttachment("text from j2k", text)
        .withAttachment("class from factory", resultClass?.text)

    resultClass.nameIdentifier?.replaced(factory.createNameIdentifier(className + "_42_"))
    val colon = resultClass.children.firstOrNull { it.elementType == KtTokens.COLON }
    if (colon != null) {
        resultClass.getSuperTypeList()?.delete()
        colon.delete()
    }

    resultClass.addSuperTypeListEntry(factory.createSuperTypeEntry(className))
    return this
}

private fun NewJavaToKotlinConverter.convertToKotlinAndFindFunction(
    referenced: PsiMethod,
    context: PsiElement,
): KtNamedFunction {
    val fakeFile = filesToKotlin(
        listOf(referenced.containingFile as PsiJavaFile),
        J2kConverterExtension.extension(useNewJ2k = true).createPostProcessor(formatCode = true),
        EmptyProgressIndicator(),
    ) { it == referenced }.results.singleOrNull() ?: error("Can't convert to Kotlin ${referenced.text}")
    val factory = KtPsiFactory(project)
    val fakeKtFile = factory.createAnalyzableFile("dummy.kt", fakeFile, context).patchFakeClass(factory)
    return fakeKtFile.findDescendantOfType {
        it.name == referenced.name && !it.textMatches(QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE)
    } ?: error("Can't find ${referenced.name} function in ${fakeKtFile.text}")
}

private val USAGE_REPLACEMENT_STRATEGY_KEY = Key<UsageReplacementStrategy>("USAGE_REPLACEMENT_STRATEGY")

private fun findOrCreateUsageReplacementStrategy(javaMethod: PsiMethod, context: PsiElement): UsageReplacementStrategy? {
    javaMethod.findUsageReplacementStrategy()?.let { return it }

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

    return createUsageReplacementStrategy(declaration, javaMethod.findExistingEditor()).also { javaMethod.setUsageReplacementStrategy(it) }
}

private fun PsiElement.findUsageReplacementStrategy(): UsageReplacementStrategy? = getUserData(USAGE_REPLACEMENT_STRATEGY_KEY)
private fun PsiElement.setUsageReplacementStrategy(strategy: UsageReplacementStrategy?) {
    putUserData(USAGE_REPLACEMENT_STRATEGY_KEY, strategy)
}

private fun unwrapUsage(usage: UsageInfo): KtReferenceExpression? {
    val ktReferenceExpression = usage.element as? KtReferenceExpression ?: return null
    return unwrapSpecialUsageOrNull(ktReferenceExpression) ?: ktReferenceExpression
}
