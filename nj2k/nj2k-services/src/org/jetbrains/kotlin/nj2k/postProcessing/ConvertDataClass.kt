/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.intentions.addUseSiteTarget
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.nj2k.NewJ2kPostProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ConvertDataClass : NewJ2kPostProcessing {
    override val writeActionNeeded: Boolean = true

    private fun KtCallableDeclaration.rename(newName: String) {
        val factory = KtPsiFactory(this)
        val escapedName = newName.escaped()
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile)).forEach {
            it.element.replace(factory.createExpression(escapedName))
        }
        setName(escapedName)
    }

    private data class DataClassInfo(
        val constructorParameter: KtParameter,
        val property: KtProperty,
        val initBlockStatement: KtBinaryExpression
    )

    private fun collectPropertiesData(klass: KtClass): List<DataClassInfo> =
        klass.getAnonymousInitializers()
            .flatMap { (it.body as KtBlockExpression).statements }
            .asSequence()
            .mapNotNull { it.asAssignment() }
            .mapNotNull { statement ->
                val property =
                    statement.left
                        ?.unpackedReferenceToProperty()
                        ?.takeIf { it.containingClass() == klass } ?: return@mapNotNull null
                if (property.getter != null || property.setter != null) return@mapNotNull null
                val constructorParameter =
                    ((statement.right as? KtReferenceExpression)
                        ?.references
                        ?.firstOrNull { it is KtSimpleNameReference }
                        ?.resolve() as? KtParameter)
                        ?.takeIf {
                            it.containingClass() == klass && !it.hasValOrVar()
                        } ?: return@mapNotNull null
                val constructorParameterType = constructorParameter.type() ?: return@mapNotNull null
                val propertyType = property.type() ?: return@mapNotNull null

                if (constructorParameterType.makeNotNullable() != propertyType.makeNotNullable()) return@mapNotNull null

                DataClassInfo(constructorParameter, property, statement)
            }.toList()

    override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
        if (element !is KtClass) return null
        return {
            val factory = KtPsiFactory(element)
            for ((constructorParameter, property, statement) in collectPropertiesData(element)) {
                constructorParameter.addBefore(property.valOrVarKeyword, constructorParameter.nameIdentifier!!)
                constructorParameter.addAfter(factory.createWhiteSpace(), constructorParameter.valOrVarKeyword!!)
                constructorParameter.rename(property.name!!)
                val propertyCommentSaver = CommentSaver(property, saveLineBreaks = true)
                if (property.modifierList != null) {
                    constructorParameter.setModifierList(property.modifierList!!)
                    constructorParameter.annotationEntries.forEach { it.delete() }
                    for (entry in constructorParameter.annotationEntries) {
                        entry.addUseSiteTarget(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER, element.project)
                    }
                }

                for (annotationEntry in property.annotationEntries) {
                    constructorParameter.addAnnotationEntry(annotationEntry).also { entry ->
                        if (entry.useSiteTarget == null) {
                            entry.addUseSiteTarget(AnnotationUseSiteTarget.FIELD, element.project)
                        }
                    }
                }
                property.delete()
                statement.delete()
                propertyCommentSaver.restore(constructorParameter, forceAdjustIndent = false)
            }
            for (initBlock in element.getAnonymousInitializers()) {
                if ((initBlock.body as KtBlockExpression).statements.isEmpty()) {
                    initBlock.delete()
                }
            }
        }
    }
}