package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.DebugUtil
import com.intellij.structuralsearch.StructuralReplaceHandler
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class KotlinReplaceHandler(private val project: Project) : StructuralReplaceHandler() {
    override fun replace(info: ReplacementInfo, options: ReplaceOptions) {
        val searchTemplate = MatcherImplUtil.createTreeFromText(
            options.matchOptions.searchPattern, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()
        val replaceTemplate = MatcherImplUtil.createTreeFromText(
            info.replacement, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()
        replaceTemplate.structuralReplace(searchTemplate, info.matchResult.match)
        CodeStyleManager.getInstance(project).reformat(replaceTemplate)
        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach { it.replace(replaceTemplate) }
    }

    // TODO add more language constructs
    private fun PsiElement.structuralReplace(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if(searchTemplate is KtDeclaration && this is KtDeclaration && match is KtDeclaration) {
            replaceDeclaration(searchTemplate, match)
            if(this is KtClassOrObject) replaceClassOrObject(searchTemplate, match)
            if(this is KtNamedFunction) replaceNamedFunction(searchTemplate, match)
        } else if (searchTemplate is KtExpression && this is KtExpression && match is KtExpression) {
            if(this is KtNamedFunction) replaceNamedFunction(searchTemplate, match)
        } else throw IllegalStateException(
            "Search template, replace template and match should either all be expressions or declarations."
        )
        return this
    }

    private fun KtDeclaration.replaceDeclaration(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
        fun KtDeclaration.replaceVisibilityModifiers(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
            if(visibilityModifierType() == null && searchTemplate.visibilityModifierType() == null) {
                match.visibilityModifierType()?.let(this::addModifier)
            }
            return this
        }

        fun KtModifierListOwner.replaceModifier(
            searchTemplate: KtModifierListOwner,
            match: KtModifierListOwner,
            modifier: KtModifierKeywordToken
        ) {
            if(!hasModifier(modifier) && match.hasModifier(modifier) && !searchTemplate.hasModifier(modifier)) {
                addModifier(modifier)
            }
        }

        replaceVisibilityModifiers(searchTemplate, match)
        replaceModifier(searchTemplate, match, KtTokens.DATA_KEYWORD)
        replaceModifier(searchTemplate, match, KtTokens.ENUM_KEYWORD)
        replaceModifier(searchTemplate, match, KtTokens.INNER_KEYWORD)
        replaceModifier(searchTemplate, match, KtTokens.SEALED_KEYWORD)
        replaceModifier(searchTemplate, match, KtTokens.ABSTRACT_KEYWORD)
        return this
    }

    private fun KtClassOrObject.replaceClassOrObject(searchTemplate: KtDeclaration, match: KtDeclaration) : PsiElement {
        if(searchTemplate is KtClassOrObject && match is KtClassOrObject) {
            if(primaryConstructor == null && searchTemplate.primaryConstructor == null) match.primaryConstructor?.let(this::add)
            if(getSuperTypeList() == null && searchTemplate.getSuperTypeList() == null) match.superTypeListEntries.forEach {
                addSuperTypeListEntry(it)
            }
            if(body == null && searchTemplate.body == null) match.body?.let(this::add)
        }
        return this
    }

    private fun KtNamedFunction.replaceNamedFunction(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        check(match is KtDeclaration) {
            "Can't replace klass $text by ${match.text} because it is not a declaration."
        }
        return this
    }
}