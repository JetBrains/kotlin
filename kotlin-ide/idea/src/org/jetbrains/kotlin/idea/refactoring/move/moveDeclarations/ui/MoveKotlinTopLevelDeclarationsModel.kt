/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameHelper
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.getOrCreateDirectory
import org.jetbrains.kotlin.idea.refactoring.move.mapWithReadActionInProcess
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MoveRefactoringDestination
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MovedEntity
import org.jetbrains.kotlin.idea.util.collectAllExpectAndActualDeclaration
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getFileOrScriptDeclarations
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Paths

internal class MoveKotlinTopLevelDeclarationsModel(
    val project: Project,
    val elementsToMove: List<KtNamedDeclaration>,
    val targetPackage: String,
    val selectedPsiDirectory: PsiDirectory?,
    val fileNameInPackage: String,
    val targetFilePath: String,
    val isMoveToPackage: Boolean,
    val isSearchReferences: Boolean,
    val isSearchInComments: Boolean,
    val isSearchInNonJavaFiles: Boolean,
    val isDeleteEmptyFiles: Boolean,
    val applyMPPDeclarations: Boolean,
    val moveCallback: MoveCallback?
) : Model {

    private inline fun <T, K> Set<T>.mapToSingleOrNull(transform: (T) -> K?): K? =
        mapTo(mutableSetOf(), transform).singleOrNull()

    private fun checkedGetSourceDirectory() = sourceFiles.mapToSingleOrNull { it.parent }
        ?: throw ConfigurationException(KotlinBundle.message("text.cannot.determine.source.directory"))

    private val sourceFiles: Set<KtFile> by lazy {
        elementsToMove.mapTo(mutableSetOf()) { it.containingKtFile }
    }

    private val elementsToMoveHasMPP by lazy {
        applyMPPDeclarations && elementsToMove.any { it.isEffectivelyActual() || it.isExpectDeclaration() }
    }

    private val singleSourceFileMode = sourceFiles.size == 1

    private fun selectPackageBasedDestination(): MoveDestination {

        val targetPackageWrapper = PackageWrapper(PsiManager.getInstance(project), targetPackage)

        return if (selectedPsiDirectory == null)
            MultipleRootsMoveDestination(targetPackageWrapper)
        else
            AutocreatingSingleSourceRootMoveDestination(targetPackageWrapper, selectedPsiDirectory.virtualFile)
    }

    private fun checkTargetFileName(fileName: String) {
        if (FileTypeManager.getInstance().getFileTypeByFileName(fileName) != KotlinFileType.INSTANCE) {
            throw ConfigurationException(KotlinBundle.message("refactoring.move.non.kotlin.file"))
        }
    }

    private fun getFilesExistingInTargetDirectory(
        targetFileName: String?,
        targetDirectory: PsiDirectory
    ): Set<PsiFile> {
        return if (targetFileName != null) {
            targetDirectory.findFile(targetFileName)?.let { setOf(it) }.orEmpty()
        } else {
            sourceFiles.mapNotNullTo(mutableSetOf()) { targetDirectory.findFile(it.name) }
        }
    }

    private fun tryMoveToPackageForExistingDirectory(targetFileName: String?, targetDirectory: PsiDirectory): KotlinMoveTarget? {

        val filesExistingInTargetDir = getFilesExistingInTargetDirectory(targetFileName, targetDirectory)

        if (filesExistingInTargetDir.isEmpty()) return null

        if (singleSourceFileMode) {
            val singeTargetFile = filesExistingInTargetDir.single() as? KtFile
            if (singeTargetFile != null) {
                return KotlinMoveTargetForExistingElement(singeTargetFile)
            }
        } else {
            val filePathsToReport = filesExistingInTargetDir.joinToString(
                separator = "\n",
                prefix = KotlinBundle.message("move.refactoring.error.text.cannot.perform.refactoring.since.the.following.files.already.exist")
            ) { it.virtualFile.path }
            throw ConfigurationException(filePathsToReport)
        }

        return null
    }

    private fun selectMoveTargetToPackage(): KotlinMoveTarget {
        val moveDestination = selectPackageBasedDestination()

        val targetDirectory: PsiDirectory?
        if (!elementsToMoveHasMPP) {
            targetDirectory = moveDestination.getTargetIfExists(checkedGetSourceDirectory())
            val targetFileName = if (singleSourceFileMode) fileNameInPackage.also(::checkTargetFileName) else null
            if (targetDirectory != null) {
                tryMoveToPackageForExistingDirectory(targetFileName, targetDirectory)?.let { return it }
            }
        } else {
            targetDirectory = null
        }

        return KotlinMoveTargetForDeferredFile(
            FqName(targetPackage),
            targetDirectory
        ) {
            val deferredFileName = if (singleSourceFileMode) fileNameInPackage else it.name
            val deferredFileDirectory = moveDestination.getTargetDirectory(it)
            getOrCreateKotlinFile(deferredFileName, deferredFileDirectory)
        }
    }

    private fun selectMoveTargetToFile(): KotlinMoveTarget {

        val targetFile = try {
            Paths.get(targetFilePath).toFile()
        } catch (e: InvalidPathException) {
            throw ConfigurationException(KotlinBundle.message("text.invalid.target.path.0", targetFilePath))
        }

        checkTargetFileName(targetFile.name)

        val jetFile = targetFile.toPsiFile(project) as? KtFile
        if (jetFile != null) {
            if (sourceFiles.singleOrNull() == jetFile) {
                throw ConfigurationException(KotlinBundle.message("text.cannot.move.to.original.file"))
            }
            return KotlinMoveTargetForExistingElement(jetFile)
        }

        val targetDirectoryPath = targetFile.toPath().parent
            ?: throw ConfigurationException(KotlinBundle.message("dialog.message.incorrect.target.path.directory.not.specified"))

        val projectBasePath = project.basePath
            ?: throw ConfigurationException(KotlinBundle.message("text.cannot.move.for.current.project"))

        if (!targetDirectoryPath.startsWith(projectBasePath)) {
            throw ConfigurationException(
                KotlinBundle.message("text.incorrect.target.path.directory.0.does.not.belong.to.current.project", targetDirectoryPath)
            )
        }

        val psiDirectory = targetDirectoryPath.toFile().toPsiDirectory(project)

        val targetPackageFqName = sourceFiles.singleOrNull()?.packageFqName
            ?: psiDirectory?.getPackage()?.let { FqName(it.qualifiedName) }
            ?: throw ConfigurationException(
                KotlinBundle.message("text.cannot.find.package.corresponding.to.0", targetDirectoryPath)
            )

        val targetDirectoryPathString = targetDirectoryPath.toString()
        val finalTargetPackageFqName = targetPackageFqName.asString()

        return KotlinMoveTargetForDeferredFile(
            targetPackageFqName,
            psiDirectory,
            targetFile = null
        ) {
            val actualPsiDirectory = psiDirectory ?: getOrCreateDirectory(targetDirectoryPathString, project)
            getOrCreateKotlinFile(targetFile.name, actualPsiDirectory, finalTargetPackageFqName)
        }
    }

    private fun verifyBeforeRun() {
        if (!isMoveToPackage && elementsToMoveHasMPP)
            throw ConfigurationException(KotlinBundle.message("text.cannot.move.expect.actual.declaration.to.file"))

        if (elementsToMove.isEmpty()) throw ConfigurationException(KotlinBundle.message("text.at.least.one.file.must.be.selected"))

        if (sourceFiles.isEmpty()) throw ConfigurationException(KotlinBundle.message("dialog.message.none.elements.were.selected"))
        if (singleSourceFileMode && fileNameInPackage.isBlank()) throw ConfigurationException(KotlinBundle.message("text.file.name.cannot.be.empty"))

        if (isMoveToPackage) {
            if (targetPackage.isNotEmpty() && !PsiNameHelper.getInstance(project).isQualifiedName(targetPackage)) {
                throw ConfigurationException(KotlinBundle.message("text.0.is.invalid.destination.package", targetPackage))
            }
        } else {
            val targetFile = File(targetFilePath).toPsiFile(project)
            if (targetFile != null && targetFile !is KtFile) {
                throw ConfigurationException(KotlinBundle.message("refactoring.move.non.kotlin.file"))
            }
        }
    }

    private fun getFUSParameters(): Pair<MovedEntity, MoveRefactoringDestination> {
        val (classType, functionType, mixedType) =
            if (elementsToMoveHasMPP)
                Triple(MovedEntity.MPPCLASSES, MovedEntity.MPPFUNCTIONS, MovedEntity.MPPMIXED)
            else
                Triple(MovedEntity.CLASSES, MovedEntity.FUNCTIONS, MovedEntity.MIXED)

        val classesAreGoingToMove = elementsToMove.any { it is KtClassOrObject }
        val functionsAreGoingToMove = elementsToMove.any { it is KtFunction }
        val entity = if (classesAreGoingToMove && functionsAreGoingToMove) mixedType else
            if (classesAreGoingToMove) classType else functionType

        val destination = if (isMoveToPackage) MoveRefactoringDestination.PACKAGE else MoveRefactoringDestination.FILE

        return entity to destination
    }


    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): ModelResultWithFUSData {

        verifyBeforeRun()

        val (entity, destination) = getFUSParameters()

        val processor = tryMoveEntireFile(throwOnConflicts) ?: moveDeclaration(throwOnConflicts)

        return ModelResultWithFUSData(processor, elementsToMove.size, entity, destination)
    }

    private fun tryMoveEntireFile(throwOnConflicts: Boolean): BaseRefactoringProcessor? {

        if (!isDeleteEmptyFiles || elementsToMoveHasMPP || !isMoveToPackage) return null

        val allDeclarationsMovingOut = elementsToMove
            .groupBy { obj: KtPureElement -> obj.containingKtFile }
            .all { it.key.getFileOrScriptDeclarations().size == it.value.size }
        if (!allDeclarationsMovingOut) return null

        val targetDirectory = selectPackageBasedDestination()
            .getTargetIfExists(checkedGetSourceDirectory())
            ?: return null

        val targetFileNameAndFile = sourceFiles
            .singleOrNull()
            ?.let { fileNameInPackage to it }
            ?.also { checkTargetFileName(it.first) }

        val filesExistingInTargetDir = getFilesExistingInTargetDirectory(targetFileNameAndFile?.first, targetDirectory)

        val moveAsFile = filesExistingInTargetDir.isEmpty() ||
                filesExistingInTargetDir.singleOrNull()?.let { sourceFiles.contains(it) } == true

        if (!moveAsFile) return null

        sourceFiles.forEach { it.updatePackageDirective = true }

        return if (targetFileNameAndFile != null)
            MoveToKotlinFileProcessor(
                project,
                targetFileNameAndFile.second,
                targetDirectory,
                targetFileNameAndFile.first,
                searchInComments = isSearchInComments,
                searchInNonJavaFiles = isSearchInNonJavaFiles,
                moveCallback = moveCallback,
                throwOnConflicts = throwOnConflicts
            )
        else
            KotlinAwareMoveFilesOrDirectoriesProcessor(
                project,
                sourceFiles.toList(),
                targetDirectory,
                isSearchReferences,
                searchInComments = isSearchInComments,
                searchInNonJavaFiles = isSearchInNonJavaFiles,
                moveCallback = moveCallback,
                throwOnConflicts = throwOnConflicts
            )
    }

    private fun moveDeclaration(throwOnConflicts: Boolean): BaseRefactoringProcessor {

        if (elementsToMoveHasMPP) require(isMoveToPackage && selectedPsiDirectory == null)

        val target = if (isMoveToPackage) selectMoveTargetToPackage() else selectMoveTargetToFile()

        val elementsWithMPPIfNeeded = if (elementsToMoveHasMPP)
            elementsToMove.mapWithReadActionInProcess(project, MoveHandler.getRefactoringName()) {
                it.collectAllExpectAndActualDeclaration()
            }.flatten().filterIsInstance<KtNamedDeclaration>()
        else elementsToMove

        for (element in elementsWithMPPIfNeeded) {
            target.verify(element.containingFile)?.let { throw ConfigurationException(it) }
        }

        val options = MoveDeclarationsDescriptor(
            project,
            MoveSource(elementsWithMPPIfNeeded),
            target,
            MoveDeclarationsDelegate.TopLevel,
            isSearchInComments,
            isSearchInNonJavaFiles,
            deleteSourceFiles = isDeleteEmptyFiles,
            moveCallback = moveCallback,
            openInEditor = false,
            allElementsToMove = null,
            analyzeConflicts = true,
            searchReferences = isSearchReferences
        )
        return MoveKotlinDeclarationsProcessor(options, Mover.Default, throwOnConflicts)
    }
}