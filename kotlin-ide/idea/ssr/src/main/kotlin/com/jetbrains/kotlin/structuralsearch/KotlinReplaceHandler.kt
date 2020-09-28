package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.structuralsearch.StructuralReplaceHandler
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.addTypeParameter
import org.jetbrains.kotlin.idea.core.setDefaultValue
import org.jetbrains.kotlin.js.translate.declaration.hasCustomGetter
import org.jetbrains.kotlin.js.translate.declaration.hasCustomSetter
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import java.lang.Integer.min

class KotlinReplaceHandler(private val project: Project) : StructuralReplaceHandler() {
    override fun replace(info: ReplacementInfo, options: ReplaceOptions) {
        val searchTemplate = PatternCompiler.compilePattern(
            project, options.matchOptions, true, true
        ).let { it.targetNode ?: it.nodes.current() }.parentIfIdentifier()
        val replaceTemplate = MatcherImplUtil.createTreeFromText(
            info.replacement, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()
        val match = info.matchResult.match.parentIfIdentifier()
        replaceTemplate.structuralReplace(searchTemplate, match)
        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach {
            it.parentIfIdentifier().replace(replaceTemplate)
        }
    }

    override fun postProcess(affectedElement: PsiElement, options: ReplaceOptions) {
        if (options.isToShortenFQN) {
            ShortenReferences.DEFAULT.process(affectedElement as KtElement)
        }
    }

    private fun PsiElement.parentIfIdentifier(): PsiElement {
        if(this is KtReferenceExpression) return parent
        return if (node.elementType == KtTokens.IDENTIFIER) parent.parentIfIdentifier() else this
    }

    private fun PsiElement.structuralReplace(searchTemplate: PsiElement, match: PsiElement): PsiElement {
        if (searchTemplate is KtDeclaration && this is KtDeclaration && match is KtDeclaration) {
            replaceDeclaration(searchTemplate, match)
            if (this is KtCallableDeclaration && searchTemplate is KtCallableDeclaration && match is KtCallableDeclaration) {
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
        } else { // KtExpression
            fixWhiteSpace(match)
        }
        return this
    }

    private fun PsiElement.fixWhiteSpace(match: PsiElement) {
        var indentationLength = Int.MAX_VALUE
        match.collectDescendantsOfType<PsiWhiteSpace> { it.text.contains("\n") }.forEach {
            indentationLength = min(indentationLength, it.text.length)
        }
        if(indentationLength > 1) indentationLength-- // exclude /n
        collectDescendantsOfType<PsiWhiteSpace> { it.text.contains("\n") }.forEach {
            it.replace(KtPsiFactory(this).createWhiteSpace("\n${" ".repeat(indentationLength + it.text.length - 1)}"))
        }
    }

    private fun KtModifierListOwner.replaceModifier(
        searchTemplate: KtModifierListOwner,
        match: KtModifierListOwner,
        modifier: KtModifierKeywordToken
    ): KtModifierListOwner {
        if (!hasModifier(modifier) && match.hasModifier(modifier) && !searchTemplate.hasModifier(modifier)) {
            addModifier(modifier)
            modifierList?.addSurroundingWhiteSpace(
                modifierList?.getModifier(modifier)!!,
                match.modifierList?.getModifier(modifier)!!
            )
        }
        return this
    }

    private fun KtModifierListOwner.fixModifierListFormatting(match: KtModifierListOwner): KtModifierListOwner {
        modifierList?.children?.let { children ->
            if (children.isNotEmpty() && children.last() is PsiWhiteSpace) children.last().delete()
        }
        modifierList?.let { rModL ->
            match.modifierList?.let { mModL ->
                addSurroundingWhiteSpace(rModL, mModL)
            }
        }
        return this
    }

    private fun KtDeclaration.replaceDeclaration(searchTemplate: KtDeclaration, match: KtDeclaration): KtDeclaration {
        fun KtDeclaration.replaceVisibilityModifiers(searchTemplate: KtDeclaration, match: KtDeclaration): PsiElement {
            if (visibilityModifierType() == null && searchTemplate.visibilityModifierType() == null) {
                match.visibilityModifierType()?.let {
                    addModifier(it)
                    modifierList?.addSurroundingWhiteSpace(visibilityModifier()!!, match.visibilityModifier()!!)
                }
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
        if (receiverTypeReference == null && searchTemplate.receiverTypeReference == null) {
            match.receiverTypeReference?.let(this::setReceiverTypeReference)
        }
        if (typeReference == null || searchTemplate.typeReference == null) {
            match.colon?.let { addFormatted(it) }
            match.typeReference?.let { addFormatted(it) }
        }
        val searchParam = searchTemplate.valueParameterList
        val matchParam = match.valueParameterList
        if (searchParam != null && matchParam != null) valueParameterList?.replaceParameterList(searchParam, matchParam)
        if (typeParameterList == null && searchTemplate.typeParameterList == null) match.typeParameters.forEach {
            addTypeParameter(it)
        }
        return this
    }

    private fun KtClassOrObject.replaceClassOrObject(searchTemplate: KtClassOrObject, match: KtClassOrObject): KtClassOrObject {
        CLASS_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        fixModifierListFormatting(match)
        val constr = primaryConstructor
        val searchContr = searchTemplate.primaryConstructor
        val matchConstr = match.primaryConstructor
        if (constr == null && searchContr == null) matchConstr?.let { addFormatted(it) }
        val paramList = getPrimaryConstructorParameterList()
        val searchParamList = searchTemplate.getPrimaryConstructorParameterList()
        val matchParamList = match.getPrimaryConstructorParameterList()
        if(searchParamList != null && matchParamList != null) paramList?.replaceParameterList(searchParamList, matchParamList)
        if (getSuperTypeList() == null && searchTemplate.getSuperTypeList() == null) match.superTypeListEntries.forEach {
            addSuperTypeListEntry(it)
        }
        if(primaryConstructorModifierList == null && searchTemplate.primaryConstructorModifierList == null) {
            match.primaryConstructorModifierList?.let { matchModList ->
                matchModList.visibilityModifierType()?.let { primaryConstructor?.addModifier(it) }
                addSurroundingWhiteSpace(primaryConstructor!!, match.primaryConstructor!!)
            }
        }
        if (body == null && searchTemplate.body == null) match.body?.let { addFormatted(it) }
        return this
    }

    private fun KtNamedFunction.replaceNamedFunction(searchTemplate: KtNamedFunction, match: KtNamedFunction): KtNamedFunction {
        FUN_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        fixModifierListFormatting(match)
        if (!hasBody() && !searchTemplate.hasBody()) {
            match.equalsToken?.let { addFormatted(it) }
            match.bodyExpression?.let { addFormatted(it) }
        }
        return this
    }

    private fun KtProperty.replaceProperty(searchTemplate: KtProperty, match: KtProperty): KtProperty {
        PROPERTY_MODIFIERS.forEach { replaceModifier(searchTemplate, match, it) }
        fixModifierListFormatting(match)
        if (!hasDelegate() && !hasInitializer()) {
            if (!searchTemplate.hasInitializer()) {
                match.equalsToken?.let { addFormatted(it) }
                match.initializer?.let { addFormatted(it) }
            }
            if (!searchTemplate.hasDelegate()) match.delegate?.let { addFormatted(it) }
        }
        if (!hasCustomGetter() && !searchTemplate.hasCustomGetter()) match.getter?.let { addFormatted(it) }
        if (!hasCustomSetter() && !searchTemplate.hasCustomSetter()) match.setter?.let { addFormatted(it) }
        return this
    }

    private fun KtParameterList.replaceParameterList(
        searchTemplate: KtParameterList,
        match: KtParameterList
    ): KtParameterList {
        parameters.forEachIndexed { i, param ->
            val searchParam = searchTemplate.parameters.getOrNull(i) ?: return@forEachIndexed
            val matchParam = match.parameters.getOrNull(i) ?: return@forEachIndexed
            if (param.typeReference == null && searchParam.typeReference == null) {
                matchParam.typeReference?.let{
                    param.typeReference = it
                    param.addSurroundingWhiteSpace(param.colon!!, matchParam.colon!!)
                }

            }
            if (!param.hasDefaultValue() && (!searchParam.hasDefaultValue())) {
                matchParam.defaultValue?.let {
                    param.setDefaultValue(it)
                    param.addSurroundingWhiteSpace(param.equalsToken!!, matchParam.equalsToken!!)
                }
            }
        }
        return this
    }

    private fun PsiElement.addFormatted(match: PsiElement) = addSurroundingWhiteSpace(add(match), match)

    private fun PsiElement.addSurroundingWhiteSpace(anchor: PsiElement, match: PsiElement) {
        val nextAnchor = anchor.nextSibling
        val prevAnchor = anchor.prevSibling
        val nextElement = match.nextSibling
        val prevElement = match.prevSibling
        if (prevElement is PsiWhiteSpace) {
            if (prevAnchor is PsiWhiteSpace) prevAnchor.replace(prevElement)
            else addBefore(prevElement, anchor)

        }
        if (nextElement is PsiWhiteSpace) {
            if (nextAnchor is PsiWhiteSpace) nextAnchor.replace(nextElement)
            else addAfter(nextElement, anchor)
        }
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