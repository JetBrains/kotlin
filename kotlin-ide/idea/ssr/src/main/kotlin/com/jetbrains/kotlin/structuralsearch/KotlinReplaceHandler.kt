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
import org.jetbrains.kotlin.idea.core.addTypeParameter
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
        println(DebugUtil.psiToString(replaceTemplate, false))
        val codeStyleManager = CodeStyleManager.getInstance(project)
        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach {
            val replacement = it.replace(replaceTemplate)
            codeStyleManager.reformat(replacement)
        }
    }

    private fun PsiElement.structuralReplace(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if(searchTemplate is KtDeclaration && this is KtDeclaration && match is KtDeclaration) {
            replaceDeclaration(searchTemplate, match)
            when(this) {
                is KtClassOrObject -> replaceClassOrObject(searchTemplate, match)
                is KtNamedFunction -> replaceNamedFunction(searchTemplate, match)
                is KtProperty -> replaceProperty(searchTemplate, match)
            }
        }
        return this
    }

    private fun KtModifierListOwner.replaceModifier(
        searchTemplate: KtModifierListOwner,
        match: KtModifierListOwner,
        modifier: KtModifierKeywordToken
    ) {
        if(!hasModifier(modifier) && match.hasModifier(modifier) && !searchTemplate.hasModifier(modifier)) addModifier(modifier)
    }

    private fun KtDeclaration.replaceDeclaration(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
        fun KtDeclaration.replaceVisibilityModifiers(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
            if(visibilityModifierType() == null && searchTemplate.visibilityModifierType() == null) {
                match.visibilityModifierType()?.let(this::addModifier)
            }
            return this
        }
        replaceVisibilityModifiers(searchTemplate, match)
        return this
    }

    private fun KtClassOrObject.replaceClassOrObject(searchTemplate: KtDeclaration, match: KtDeclaration) : PsiElement {
        if(searchTemplate is KtClassOrObject && match is KtClassOrObject) {
            if(primaryConstructor == null && searchTemplate.primaryConstructor == null) match.primaryConstructor?.let(this::add)
            if(getSuperTypeList() == null && searchTemplate.getSuperTypeList() == null) match.superTypeListEntries.forEach {
                addSuperTypeListEntry(it)
            }
            if(body == null && searchTemplate.body == null) match.body?.let(this::add)
            CLASS_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        }
        return this
    }

    private fun KtNamedFunction.replaceNamedFunction(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if(searchTemplate is KtNamedFunction && match is KtNamedFunction) {
            if(typeParameterList == null && searchTemplate.typeParameterList == null) match.typeParameters.forEach {
                addTypeParameter(it)
            }
            if(valueParameterList == null && searchTemplate.valueParameterList == null) match.valueParameters.forEach {
                match.valueParameters.add(it)
            }
            if(!hasBody() && !searchTemplate.hasBody()) match.bodyExpression?.let(this::add)
            FUN_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        }
        return this
    }

    private fun KtProperty.replaceProperty(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if(searchTemplate is KtProperty && match is KtProperty) {
            if(typeReference == null || searchTemplate.typeReference == null) match.typeReference?.let(this::setTypeReference)
            if(!hasDelegate() && !hasInitializer()) {
                if(!searchTemplate.hasInitializer()) initializer = match.initializer
                if(!searchTemplate.hasDelegate()) match.delegate?.let(this::add)
            }
            PROPERTY_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        }
        return this
    }

    companion object {
        private val CLASS_MODIFIERS = arrayOf(
            KtTokens.ABSTRACT_KEYWORD,
            KtTokens.ENUM_KEYWORD,
            KtTokens.OPEN_KEYWORD,
            KtTokens.INNER_KEYWORD,
            KtTokens.FINAL_KEYWORD,
            KtTokens.COMPANION_KEYWORD,
            KtTokens.SEALED_KEYWORD,
            KtTokens.DATA_KEYWORD,
            KtTokens.INLINE_KEYWORD,
            KtTokens.EXTERNAL_KEYWORD,
            KtTokens.ANNOTATION_KEYWORD,
            KtTokens.CROSSINLINE_KEYWORD,
            KtTokens.HEADER_KEYWORD,
            KtTokens.IMPL_KEYWORD,
            KtTokens.EXPECT_KEYWORD,
            KtTokens.ACTUAL_KEYWORD
        )

        private val FUN_MODIFIERS = arrayOf(
            KtTokens.ABSTRACT_KEYWORD,
            KtTokens.OPEN_KEYWORD,
            KtTokens.INNER_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD,
            KtTokens.FINAL_KEYWORD,
            KtTokens.INLINE_KEYWORD,
            KtTokens.TAILREC_KEYWORD,
            KtTokens.EXTERNAL_KEYWORD,
            KtTokens.OPERATOR_KEYWORD,
            KtTokens.INFIX_KEYWORD,
            KtTokens.SUSPEND_KEYWORD,
            KtTokens.HEADER_KEYWORD,
            KtTokens.IMPL_KEYWORD,
            KtTokens.EXPECT_KEYWORD,
            KtTokens.ACTUAL_KEYWORD
        )

        private val PROPERTY_MODIFIERS = arrayOf(
            KtTokens.ABSTRACT_KEYWORD,
            KtTokens.OPEN_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD,
            KtTokens.FINAL_KEYWORD,
            KtTokens.LATEINIT_KEYWORD,
            KtTokens.EXTERNAL_KEYWORD,
            KtTokens.CONST_KEYWORD,
            KtTokens.HEADER_KEYWORD,
            KtTokens.IMPL_KEYWORD,
            KtTokens.EXPECT_KEYWORD,
            KtTokens.ACTUAL_KEYWORD
        )
    }
}