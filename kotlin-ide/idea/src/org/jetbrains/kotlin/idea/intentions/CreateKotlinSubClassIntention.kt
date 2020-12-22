/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaDirectoryService
import com.intellij.refactoring.rename.PsiElementRenameHandler
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.ui.CreateKotlinClassDialog
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.ClassHeaderBuilder
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.ModifiersChecker

private const val IMPL_SUFFIX = "Impl"

class CreateKotlinSubClassIntention : SelfTargetingRangeIntention<KtClass>(
    KtClass::class.java,
    KotlinBundle.lazyMessage("create.kotlin.subclass")
) {

    override fun applicabilityRange(element: KtClass): TextRange? {
        if (element.name == null || element.getParentOfType<KtFunction>(true) != null) {
            // Local / anonymous classes are not supported
            return null
        }
        if (!element.isInterface() && !element.isSealed() && !element.isAbstract() && !element.hasModifier(KtTokens.OPEN_KEYWORD)) {
            return null
        }
        val primaryConstructor = element.primaryConstructor
        if (!element.isInterface() && primaryConstructor != null) {
            val constructors = element.secondaryConstructors + primaryConstructor
            if (constructors.none {
                    !it.isPrivate() && it.valueParameters.all { parameter -> parameter.hasDefaultValue() }
                }) {
                // At this moment we require non-private default constructor
                // TODO: handle non-private constructors with parameters
                return null
            }
        }

        setTextGetter(getImplementTitle(element))
        return TextRange(element.startOffset, element.body?.lBrace?.startOffset ?: element.endOffset)
    }

    private fun getImplementTitle(baseClass: KtClass) = when {
        baseClass.isInterface() -> KotlinBundle.lazyMessage("implement.interface")
        baseClass.isAbstract() -> KotlinBundle.lazyMessage("implement.abstract.class")
        baseClass.isSealed() -> KotlinBundle.lazyMessage("implement.sealed.class")
        else /* open class */ -> KotlinBundle.lazyMessage("create.subclass")
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtClass, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")

        val name = element.name ?: throw IllegalStateException("This intention should not be applied to anonymous classes")
        if (element.isSealed() && !element.languageVersionSettings.supportsFeature(LanguageFeature.SealedInterfaces)) {
            createNestedSubclass(element, name, editor)
        } else {
            createExternalSubclass(element, name, editor)
        }
    }

    private fun defaultTargetName(baseName: String) = "$baseName$IMPL_SUFFIX"

    private fun KtClassOrObject.hasSameDeclaration(name: String) = declarations.any { it is KtNamedDeclaration && it.name == name }

    private fun targetNameWithoutConflicts(baseName: String, container: KtClassOrObject?) =
        KotlinNameSuggester.suggestNameByName(defaultTargetName(baseName)) { container?.hasSameDeclaration(it) != true }

    private fun createNestedSubclass(sealedClass: KtClass, sealedName: String, editor: Editor) {
        val project = sealedClass.project
        val klass = runWriteAction {
            val builder = buildClassHeader(targetNameWithoutConflicts(sealedName, sealedClass), sealedClass, sealedName)
            val classFromText = KtPsiFactory(project).createClass(builder.asString())
            val body = sealedClass.getOrCreateBody()
            body.addBefore(classFromText, body.rBrace) as KtClass
        }
        runInteractiveRename(klass, project, sealedClass, editor)
        chooseAndImplementMethods(project, klass, editor)
    }

    private fun createExternalSubclass(baseClass: KtClass, baseName: String, editor: Editor) {
        var container: KtClassOrObject = baseClass
        var name = baseName
        var visibility = ModifiersChecker.resolveVisibilityFromModifiers(baseClass, DescriptorVisibilities.PUBLIC)
        while (!container.isPrivate() && !container.isProtected() && !(container is KtClass && container.isInner())) {
            val parent = container.containingClassOrObject
            if (parent != null) {
                val parentName = parent.name
                if (parentName != null) {
                    container = parent
                    name = "$parentName.$name"
                    val parentVisibility = ModifiersChecker.resolveVisibilityFromModifiers(parent, visibility)
                    if (DescriptorVisibilities.compare(parentVisibility, visibility) ?: 0 < 0) {
                        visibility = parentVisibility
                    }
                }
            }
            if (container != parent) {
                break
            }
        }
        val project = baseClass.project
        val factory = KtPsiFactory(project)
        if (container.containingClassOrObject == null && !ApplicationManager.getApplication().isUnitTestMode) {
            val dlg = chooseSubclassToCreate(baseClass, baseName) ?: return
            val targetName = dlg.className
            val (file, klass) = runWriteAction {
                val file = getOrCreateKotlinFile("$targetName.kt", dlg.targetDirectory!!)!!
                val builder = buildClassHeader(targetName, baseClass, baseClass.fqName!!.asString())
                file.add(factory.createClass(builder.asString()))
                val klass = file.getChildOfType<KtClass>()!!
                ShortenReferences.DEFAULT.process(klass)
                file to klass
            }
            chooseAndImplementMethods(project, klass, CodeInsightUtil.positionCursor(project, file, klass) ?: editor)
        } else {
            val klass = runWriteAction {
                val builder = buildClassHeader(
                    targetNameWithoutConflicts(baseName, baseClass.containingClassOrObject),
                    baseClass, name, visibility
                )
                val classFromText = factory.createClass(builder.asString())
                container.parent.addAfter(classFromText, container) as KtClass
            }
            runInteractiveRename(klass, project, container, editor)
            chooseAndImplementMethods(project, klass, editor)
        }
    }

    private fun runInteractiveRename(klass: KtClass, project: Project, container: KtClassOrObject, editor: Editor) {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        PsiElementRenameHandler.rename(klass, project, container, editor)
    }

    private fun chooseSubclassToCreate(baseClass: KtClass, baseName: String): CreateKotlinClassDialog? {
        val sourceDir = baseClass.containingFile.containingDirectory

        val aPackage = JavaDirectoryService.getInstance().getPackage(sourceDir)
        val dialog = object : CreateKotlinClassDialog(
            baseClass.project, text,
            targetNameWithoutConflicts(baseName, baseClass.containingClassOrObject),
            aPackage?.qualifiedName ?: "",
            CreateClassKind.CLASS, true,
            ModuleUtilCore.findModuleForPsiElement(baseClass),
            baseClass.isSealed()
        ) {
            override fun getBaseDir(packageName: String?) = sourceDir

            override fun reportBaseInTestSelectionInSource() = true
        }
        return if (!dialog.showAndGet() || dialog.targetDirectory == null) null else dialog
    }

    private fun buildClassHeader(
        targetName: String,
        baseClass: KtClass,
        baseName: String,
        defaultVisibility: DescriptorVisibility = ModifiersChecker.resolveVisibilityFromModifiers(baseClass, DescriptorVisibilities.PUBLIC)
    ): ClassHeaderBuilder {
        return ClassHeaderBuilder().apply {
            if (!baseClass.isInterface()) {
                if (defaultVisibility != DescriptorVisibilities.PUBLIC) {
                    modifier(defaultVisibility.name)
                }
                if (baseClass.isInner()) {
                    modifier(KtTokens.INNER_KEYWORD.value)
                }
            }
            name(targetName)
            val typeParameters = baseClass.typeParameterList?.parameters
            typeParameters(typeParameters?.map { it.text }.orEmpty())
            baseClass(baseName, typeParameters?.map { it.name ?: "" }.orEmpty(), baseClass.isInterface())
            typeConstraints(baseClass.typeConstraintList?.constraints?.map { it.text }.orEmpty())
        }
    }

    private fun chooseAndImplementMethods(project: Project, targetClass: KtClass, editor: Editor) {
        editor.caretModel.moveToOffset(targetClass.textRange.startOffset)
        ImplementMembersHandler().invoke(project, editor, targetClass.containingFile)
    }
}
