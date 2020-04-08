/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.Modality.ABSTRACT
import org.jetbrains.kotlin.descriptors.Modality.SEALED
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.supertypes

class AddPropertyToSupertypeFix private constructor(
    element: KtProperty,
    private val properties: List<PropertyData>
) : KotlinQuickFixAction<KtProperty>(element), LowPriorityAction {

    private class PropertyData(val signaturePreview: String, val sourceCode: String, val targetClass: KtClass)

    override fun getText(): String {
        val single = properties.singleOrNull()
        return if (single != null) actionName(single) else KotlinBundle.message("fix.add.property.to.supertype.text.generic")
    }

    override fun getFamilyName() = KotlinBundle.message("fix.add.property.to.supertype.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (properties.size == 1 || editor == null || !editor.component.isShowing) {
                addProperty(properties.first(), project)
            } else {
                JBPopupFactory.getInstance().createListPopup(createPropertyPopup(project)).showInBestPositionFor(editor)
            }
        }
    }

    private fun addProperty(propertyData: PropertyData, project: Project) {
        project.executeWriteCommand(KotlinBundle.message("fix.add.property.to.supertype.progress")) {
            val classBody = propertyData.targetClass.getOrCreateBody()

            val propertyElement = KtPsiFactory(project).createProperty(propertyData.sourceCode)
            val insertedPropertyElement = classBody.addBefore(propertyElement, classBody.rBrace) as KtProperty

            ShortenReferences.DEFAULT.process(insertedPropertyElement)
            val modifierToken = insertedPropertyElement.modalityModifier()?.node?.elementType as? KtModifierKeywordToken
                ?: return@executeWriteCommand
            if (insertedPropertyElement.implicitModality() == modifierToken) {
                RemoveModifierFix(insertedPropertyElement, modifierToken, true).invoke()
            }
        }
    }

    private fun createPropertyPopup(project: Project): ListPopupStep<*> {
        return object : BaseListPopupStep<PropertyData>(KotlinBundle.message("fix.add.property.to.supertype.choose.type"), properties) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: PropertyData, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    addProperty(selectedValue, project)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(value: PropertyData) = PlatformIcons.PROPERTY_ICON
            override fun getTextFor(value: PropertyData) = actionName(value)
        }
    }

    private fun actionName(propertyData: PropertyData): String {
        return KotlinBundle.message("fix.add.property.to.supertype.text", propertyData.signaturePreview, propertyData.targetClass.name.toString())
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val property = diagnostic.psiElement as? KtProperty ?: return null

            val descriptors = generatePropertiesToAdd(property)
            if (descriptors.isEmpty()) return null

            val project = diagnostic.psiFile.project
            val propertyData = descriptors.mapNotNull { createPropertyData(it, property.initializer, project) }
            if (propertyData.isEmpty()) return null

            return AddPropertyToSupertypeFix(property, propertyData)
        }

        private fun createPropertyData(
            propertyDescriptor: PropertyDescriptor,
            initializer: KtExpression?,
            project: Project
        ): PropertyData? {
            val classDescriptor = propertyDescriptor.containingDeclaration as ClassDescriptor
            var signaturePreview = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.render(propertyDescriptor)
            if (classDescriptor.kind == ClassKind.INTERFACE) {
                signaturePreview = signaturePreview.substring("abstract ".length)
            }

            var sourceCode = IdeDescriptorRenderers.SOURCE_CODE.render(propertyDescriptor)
            if (classDescriptor.kind == ClassKind.CLASS && classDescriptor.modality == Modality.OPEN && initializer != null) {
                sourceCode += " = ${initializer.text}"
            }

            val targetClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor) as? KtClass ?: return null
            return PropertyData(signaturePreview, sourceCode, targetClass)
        }

        private fun generatePropertiesToAdd(propertyElement: KtProperty): List<PropertyDescriptor> {
            val propertyDescriptor =
                propertyElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? PropertyDescriptor ?: return emptyList()
            val classDescriptor = propertyDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            return getSuperClasses(classDescriptor)
                .asSequence()
                .filterNot { KotlinBuiltIns.isAnyOrNullableAny(it.defaultType) }
                .map { generatePropertySignatureForType(propertyDescriptor, it) }
                .toList()
        }

        private fun getSuperClasses(classDescriptor: ClassDescriptor): List<ClassDescriptor> {
            val supertypes = classDescriptor.defaultType.supertypes().toMutableList().sortSubtypesFirst()
            return supertypes.mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
        }

        private fun MutableList<KotlinType>.sortSubtypesFirst(): List<KotlinType> {
            // TODO: rewrite this
            val typeChecker = KotlinTypeChecker.DEFAULT
            for (i in 1 until size) {
                val currentType = this[i]
                for (j in 0 until i) {
                    if (typeChecker.isSubtypeOf(currentType, this[j])) {
                        this.removeAt(i)
                        this.add(j, currentType)
                        break
                    }
                }
            }
            return this
        }

        private fun generatePropertySignatureForType(
            propertyDescriptor: PropertyDescriptor,
            typeDescriptor: ClassDescriptor
        ): PropertyDescriptor {
            val containerModality = typeDescriptor.modality
            val modality = if (containerModality == SEALED || containerModality == ABSTRACT) ABSTRACT else propertyDescriptor.modality
            return propertyDescriptor.newCopyBuilder()
                .setOwner(typeDescriptor)
                .setModality(modality)
                .setVisibility(propertyDescriptor.visibility)
                .setKind(CallableMemberDescriptor.Kind.DECLARATION)
                .setCopyOverrides(false)
                .build()!!
        }
    }
}