/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.util.RefactoringMessageUtil
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.getTargetPackageFqName
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.roots.getSuitableDestinationSourceRoots
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MoveRefactoringDestination
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MovedEntity
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal abstract class MoveKotlinNestedClassesToUpperLevelModel(
    val project: Project,
    val innerClass: KtClassOrObject,
    val target: PsiElement,
    val parameter: String?,
    val className: String,
    val passOuterClass: Boolean,
    val searchInComments: Boolean,
    val isSearchInNonJavaFiles: Boolean,
    val packageName: String,
    val isOpenInEditor: Boolean
) : Model {

    protected abstract fun chooseSourceRoot(
        newPackage: PackageWrapper,
        contentSourceRoots: List<VirtualFile>,
        initialDir: PsiDirectory?
    ): VirtualFile?

    private val innerClassDescriptor = innerClass.resolveToDescriptorIfAny(BodyResolveMode.FULL)

    private fun getTargetContainer(): PsiElement? {
        if (target is PsiDirectory) {
            val oldPackageFqName = getTargetPackageFqName(target)
            val targetName = packageName
            if (oldPackageFqName?.asString() != targetName) {
                val projectRootManager = ProjectRootManager.getInstance(project)

                val contentSourceRoots = getSuitableDestinationSourceRoots(project)
                val newPackage = PackageWrapper(PsiManager.getInstance(project), targetName)

                val targetSourceRoot: VirtualFile
                if (contentSourceRoots.size > 1) {
                    val oldPackage = oldPackageFqName?.let {
                        JavaPsiFacade.getInstance(project).findPackage(it.asString())
                    }

                    var initialDir: PsiDirectory? = null
                    if (oldPackage != null) {
                        val root = projectRootManager.fileIndex.getContentRootForFile(target.virtualFile)
                        initialDir = oldPackage.directories.firstOrNull {
                            Comparing.equal(projectRootManager.fileIndex.getContentRootForFile(it.virtualFile), root)
                        }
                    }

                    targetSourceRoot = chooseSourceRoot(newPackage, contentSourceRoots, initialDir) ?: return null
                } else {
                    targetSourceRoot = contentSourceRoots[0]
                }

                RefactoringUtil.findPackageDirectoryInSourceRoot(newPackage, targetSourceRoot)?.let { return it }

                return runWriteAction {
                    try {
                        RefactoringUtil.createPackageDirectoryInSourceRoot(newPackage, targetSourceRoot)
                    } catch (e: IncorrectOperationException) {
                        null
                    }
                }
            }

            return target
        }

        return target as? KtFile ?: target as? KtClassOrObject
    }

    @Throws(ConfigurationException::class)
    private fun getTargetContainerWithValidation(): PsiElement {

        if (className.isEmpty()) {
            throw ConfigurationException(JavaRefactoringBundle.message("no.class.name.specified"))
        }
        if (!className.isIdentifier()) {
            throw ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(className))
        }

        if (passOuterClass) {
            if (parameter.isNullOrEmpty()) {
                throw ConfigurationException(JavaRefactoringBundle.message("no.parameter.name.specified"))
            }
            if (!parameter.isIdentifier()) {
                throw ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(parameter))
            }
        }

        val targetContainer = getTargetContainer()

        if (targetContainer is KtClassOrObject) {
            val targetClass = targetContainer as KtClassOrObject?
            if (targetClass != null) {
                for (member in targetClass.declarations) {
                    if (member is KtClassOrObject && className == member.getName()) {
                        throw ConfigurationException(JavaRefactoringBundle.message("inner.class.exists", className, targetClass.name))
                    }
                }
            }
        }

        if (targetContainer is PsiDirectory || targetContainer is KtFile) {
            val targetPackageFqName = getTargetPackageFqName(target)
                ?: throw ConfigurationException(KotlinBundle.message("text.no.package.corresponds.to.directory"))

            val existingClass = innerClassDescriptor?.let { DescriptorUtils.getContainingModule(it) }
                ?.getPackage(targetPackageFqName)
                ?.memberScope
                ?.getContributedClassifier(Name.identifier(className), NoLookupLocation.FROM_IDE)
            if (existingClass != null) {
                throw ConfigurationException(
                    KotlinBundle.message("text.class.0.already.exists.in.package.1", className, targetPackageFqName)
                )
            }

            val targetDir = targetContainer as? PsiDirectory ?: targetContainer.containingFile.containingDirectory
            val message = RefactoringMessageUtil.checkCanCreateFile(targetDir, "$className.kt")
            if (message != null) throw ConfigurationException(message)
        }

        return targetContainer ?: throw ConfigurationException(KotlinBundle.message("text.invalid.target.specified"))
    }

    @Throws(ConfigurationException::class)
    private fun getMoveTarget(): Pair<KotlinMoveTarget, MoveRefactoringDestination> {
        val target = getTargetContainerWithValidation()
        return if (target is PsiDirectory) {
            val targetPackageFqName = getTargetPackageFqName(target)
                ?: throw ConfigurationException(KotlinBundle.message("text.cannot.find.target.package.name"))

            val suggestedName = KotlinNameSuggester.suggestNameByName(className) {
                target.findFile(it + "." + KotlinFileType.EXTENSION) == null
            }

            val targetFileName = suggestedName + "." + KotlinFileType.EXTENSION

            val target = KotlinMoveTargetForDeferredFile(
                targetPackageFqName,
                target,
                targetFile = null
            ) { createKotlinFile(targetFileName, target, targetPackageFqName.asString()) }

            target to MoveRefactoringDestination.FILE
        } else {
            KotlinMoveTargetForExistingElement(target as KtElement) to MoveRefactoringDestination.DECLARATION
        }
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): ModelResultWithFUSData {

        val moveTarget = getMoveTarget()

        val outerInstanceParameterName = if (passOuterClass) packageName else null
        val delegate = MoveDeclarationsDelegate.NestedClass(className, outerInstanceParameterName)
        val moveDescriptor = MoveDeclarationsDescriptor(
            project,
            MoveSource(innerClass),
            moveTarget.first,
            delegate,
            searchInComments,
            isSearchInNonJavaFiles,
            deleteSourceFiles = false,
            moveCallback = null,
            openInEditor = isOpenInEditor
        )

        val processor = MoveKotlinDeclarationsProcessor(moveDescriptor, Mover.Default, throwOnConflicts)

        return ModelResultWithFUSData(
          processor = processor,
          elementsCount = 1,
          entityToMove = MovedEntity.CLASSES,
          destination = moveTarget.second
        )
    }
}
