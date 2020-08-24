package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.structuralsearch.StructuralReplaceHandler
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo
import org.jetbrains.kotlin.idea.core.addTypeParameter
import org.jetbrains.kotlin.idea.core.setDefaultValue
import org.jetbrains.kotlin.js.translate.declaration.hasCustomGetter
import org.jetbrains.kotlin.js.translate.declaration.hasCustomSetter
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference

class KotlinReplaceHandler(private val project: Project) : StructuralReplaceHandler() {
    override fun replace(info: ReplacementInfo, options: ReplaceOptions) {
        val searchTemplate = PatternCompiler.compilePattern(project, options.matchOptions, true, true).nodes.current()
        val replaceTemplate = MatcherImplUtil.createTreeFromText(
            info.replacement, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()
        replaceTemplate.structuralReplace(searchTemplate, info.matchResult.match)
        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach {
            it.replace(replaceTemplate)
        }
    }

    private fun PsiElement.structuralReplace(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if(searchTemplate is KtDeclaration && this is KtDeclaration && match is KtDeclaration) {
            replaceDeclaration(searchTemplate, match)
            if(this is KtCallableDeclaration && searchTemplate is KtCallableDeclaration && match is KtCallableDeclaration) {
                replaceCallableDeclaration(searchTemplate, match)
            }
            when {
                this is KtClassOrObject && searchTemplate is KtClassOrObject && match is KtClassOrObject ->
                    replaceClassOrObject(searchTemplate, match)
                this is KtNamedFunction && searchTemplate is KtNamedFunction && match is KtNamedFunction ->
                    replaceNamedFunction(searchTemplate, match)
                this is KtProperty && searchTemplate is KtProperty && match is KtProperty ->
                    replaceProperty(searchTemplate, match)
            }
        }
        return this
    }

    private fun KtModifierListOwner.replaceModifier(
        searchTemplate: KtModifierListOwner,
        match: KtModifierListOwner,
        modifier: KtModifierKeywordToken
    ): KtModifierListOwner {
        if(!hasModifier(modifier) && match.hasModifier(modifier) && !searchTemplate.hasModifier(modifier)) addModifier(modifier)
        return this
    }

    private fun KtDeclaration.replaceDeclaration(searchTemplate: KtDeclaration, match: KtDeclaration): KtDeclaration {
        fun KtDeclaration.replaceVisibilityModifiers(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
            if(visibilityModifierType() == null && searchTemplate.visibilityModifierType() == null) {
                match.visibilityModifierType()?.let(this::addModifier)
            }
            return this
        }
        replaceVisibilityModifiers(searchTemplate, match)
        return this
    }

    private fun KtCallableDeclaration.replaceCallableDeclaration(
        searchTemplate: KtCallableDeclaration,
        match: KtCallableDeclaration
    ): KtCallableDeclaration {
        if(receiverTypeReference == null && searchTemplate.receiverTypeReference == null) {
            match.receiverTypeReference?.let(this::setReceiverTypeReference)
        }
        if(typeReference == null || searchTemplate.typeReference == null) match.typeReference?.let(this::setTypeReference)
        if(valueParameterList == null && searchTemplate.valueParameterList == null) match.valueParameters.forEach {
            match.valueParameters.add(it)
        }
        if(typeParameterList == null && searchTemplate.typeParameterList == null) match.typeParameters.forEach {
            addTypeParameter(it)
        }
        return this
    }

    private fun KtClassOrObject.replaceClassOrObject(searchTemplate: KtClassOrObject, match: KtClassOrObject) : KtClassOrObject {
        CLASS_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        if(primaryConstructor == null && searchTemplate.primaryConstructor == null) match.primaryConstructor?.let(this::add)
        if(getSuperTypeList() == null && searchTemplate.getSuperTypeList() == null) match.superTypeListEntries.forEach {
            addSuperTypeListEntry(it)
        }
        if(body == null && searchTemplate.body == null) match.body?.let(this::add)
        return this
    }

    private fun KtNamedFunction.replaceNamedFunction(searchTemplate: KtNamedFunction, match: KtNamedFunction): KtNamedFunction {
        FUN_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        val searchParam = searchTemplate.valueParameterList
        val matchParam = match.valueParameterList
        if(searchParam != null && matchParam != null) valueParameterList?.replaceParameterList(searchParam, matchParam)
        if(!hasBody() && !searchTemplate.hasBody()) {
            match.equalsToken?.let(this::add)
            match.bodyExpression?.let(this::add)
        }
        return this
    }

    private fun KtProperty.replaceProperty(searchTemplate: KtProperty, match: KtProperty): KtProperty {
        PROPERTY_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        if(receiverTypeReference == null && searchTemplate.receiverTypeReference == null) {
            match.receiverTypeReference?.let(this::setReceiverTypeReference)
        }
        if(typeReference == null && searchTemplate.typeReference == null) match.typeReference?.let(this::setTypeReference)
        if(!hasDelegate() && !hasInitializer()) {
            if(!searchTemplate.hasInitializer()) initializer = match.initializer
            if(!searchTemplate.hasDelegate()) match.delegate?.let(this::add)
        }
        if(!hasCustomGetter() && !searchTemplate.hasCustomGetter()) match.getter?.let(this::add)
        if(!hasCustomSetter() && !searchTemplate.hasCustomSetter()) match.setter?.let(this::add)
        return this
    }

    private fun KtParameterList.replaceParameterList(
        searchTemplate: KtParameterList,
        match: KtParameterList
    ): KtParameterList {
        parameters.forEachIndexed { i, param ->
            val searchParam = searchTemplate.parameters.getOrNull(i)
            val matchParam = match.parameters.getOrNull(i)
            if(param.typeReference == null && searchParam?.typeReference == null) {
                matchParam?.typeReference?.let(param::setTypeReference)
            }
            if(!param.hasDefaultValue() && (searchParam == null || !searchParam.hasDefaultValue())) {
                matchParam?.defaultValue?.let(param::setDefaultValue)
            }
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