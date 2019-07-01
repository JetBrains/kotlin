/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.intentions.addUseSiteTarget
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.escaped
import org.jetbrains.kotlin.nj2k.postProcessing.ElementsBasedPostProcessing
import org.jetbrains.kotlin.nj2k.postProcessing.descendantsOfType
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.nj2k.postProcessing.unpackedReferenceToProperty
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ConvertToDataClassProcessing : ElementsBasedPostProcessing() {
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
                if (property.initializer != null) return@mapNotNull null
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

                DataClassInfo(
                    constructorParameter,
                    property,
                    statement
                )
            }.toList()

    override fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext) {
        for (klass in elements.descendantsOfType<KtClass>()) {
            convertClass(klass)
        }
    }

    private fun convertClass(klass: KtClass) {
        val factory = KtPsiFactory(klass)
        for ((constructorParameter, property, statement) in collectPropertiesData(klass)) {
            constructorParameter.addBefore(property.valOrVarKeyword, constructorParameter.nameIdentifier!!)
            constructorParameter.addAfter(factory.createWhiteSpace(), constructorParameter.valOrVarKeyword!!)
            constructorParameter.rename(property.name!!)
            val propertyCommentSaver = CommentSaver(property, saveLineBreaks = true)

            constructorParameter.setVisibility(property.visibilityModifierTypeOrDefault())
            for (annotationEntry in constructorParameter.annotationEntries) {
                if (annotationEntry.useSiteTarget == null) {
                    annotationEntry.addUseSiteTarget(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER, klass.project)
                }
            }

            for (annotationEntry in property.annotationEntries) {
                constructorParameter.addAnnotationEntry(annotationEntry).also { entry ->
                    if (entry.useSiteTarget == null) {
                        entry.addUseSiteTarget(AnnotationUseSiteTarget.FIELD, klass.project)
                    }
                }
            }
            property.delete()
            statement.delete()
            propertyCommentSaver.restore(constructorParameter, forceAdjustIndent = false)
        }

        for (initBlock in klass.getAnonymousInitializers()) {
            if ((initBlock.body as KtBlockExpression).statements.isEmpty()) {
                val commentSaver = CommentSaver(initBlock)
                initBlock.delete()
                klass.primaryConstructor?.let { commentSaver.restore(it) }
            }
        }
    }
}