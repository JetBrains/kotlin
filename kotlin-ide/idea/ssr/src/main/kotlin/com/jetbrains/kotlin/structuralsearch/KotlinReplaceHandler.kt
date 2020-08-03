package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.structuralsearch.StructuralReplaceHandler
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class KotlinReplaceHandler(private val project: Project) : StructuralReplaceHandler() {
    override fun replace(info: ReplacementInfo, options: ReplaceOptions) {
        val replaceTemplate = MatcherImplUtil.createTreeFromText(
            info.replacement, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()
        val match = info.getMatch(0) ?: throw IllegalStateException("No match found.")
        replaceTemplate.structuralReplace(match)
        CodeStyleManager.getInstance(project).reformat(replaceTemplate)
        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach { it.replace(replaceTemplate) }
    }

    // TODO add more language constructs
    private fun PsiElement.structuralReplace(match: PsiElement): PsiElement = when(this) {
        is KtClass -> replaceClass(match)
        is KtObjectDeclaration -> replaceObject(match)
        else -> match
    }

    private fun KtClass.replaceClass(match: PsiElement): PsiElement {
        check(match is KtDeclaration) {
            "Can't replace klass $text by ${match.text} because it is not a declaration."
        }
        if(match !is KtClass) return replaceDeclaration(match)

        fun KtClass.replaceModifiers() {
            if(!isData() && match.isData()) addModifier(KtTokens.DATA_KEYWORD)
            if(!isEnum() && match.isEnum()) addModifier(KtTokens.ENUM_KEYWORD)
            if(!isInner() && match.isInner()) addModifier(KtTokens.INNER_KEYWORD)
            if(!isSealed() && match.isSealed()) addModifier(KtTokens.SEALED_KEYWORD)
            if(!isAbstract() && match.isAbstract()) addModifier(KtTokens.ABSTRACT_KEYWORD)
        }
        replaceClassOrObject(match)
        replaceModifiers()
        return this
    }

    private fun KtObjectDeclaration.replaceObject(match: PsiElement): PsiElement {
        replaceClassOrObject(match)
        return this
    }

    private fun KtClassOrObject.replaceClassOrObject(match: PsiElement) : PsiElement {
        check(match is KtDeclaration) {
            "Can't replace klass $text by ${match.text} because it is not a declaration."
        }
        if(match !is KtClassOrObject) return replaceDeclaration(match)

        fun KtClassOrObject.replacePrimaryConstructor() {
            if(primaryConstructor == null) match.primaryConstructor?.let(this::add)
        }

        fun KtClassOrObject.replaceSuperTypeList() {
            if(getSuperTypeList() == null) match.superTypeListEntries.forEach { addSuperTypeListEntry(it) }
        }

        fun KtClassOrObject.replaceBody() {
            if(body == null) match.body?.let(this::add)
        }

        replaceDeclaration(match)
        replacePrimaryConstructor()
        replaceSuperTypeList()
        replaceBody()
        return this
    }
    
    private fun KtDeclaration.replaceDeclaration(match: KtDeclaration): PsiElement {
        replaceVisibilityModifiers(match)
        return this
    }
    
    private fun KtDeclaration.replaceVisibilityModifiers(match: KtDeclaration): PsiElement {
        if(visibilityModifierType() == null) {
            match.visibilityModifierType()?.let(this::addModifier)
        }
        return this
    }
}