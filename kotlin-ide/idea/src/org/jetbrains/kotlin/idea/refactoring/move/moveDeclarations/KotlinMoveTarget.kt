/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.util.*

interface KotlinMoveTarget {
    val targetContainerFqName: FqName?
    val targetFile: VirtualFile?

    fun getOrCreateTargetPsi(originalPsi: PsiElement): KtElement?
    fun getTargetPsiIfExists(originalPsi: PsiElement): KtElement?

    // Check possible errors and return corresponding message, or null if no errors are detected
    fun verify(file: PsiFile): String?

    val targetScope: VirtualFile?
        get() = targetFile
}

interface KotlinDirectoryBasedMoveTarget : KotlinMoveTarget {
    val directory: PsiDirectory?

    override val targetScope: VirtualFile?
        get() = super.targetScope ?: directory?.virtualFile
}

object EmptyKotlinMoveTarget : KotlinMoveTarget {
    override val targetContainerFqName: FqName? = null
    override val targetFile: VirtualFile? = null

    override fun getOrCreateTargetPsi(originalPsi: PsiElement): KtElement? = null
    override fun getTargetPsiIfExists(originalPsi: PsiElement): KtElement? = null
    override fun verify(file: PsiFile): String? = null
}

class KotlinMoveTargetForExistingElement(val targetElement: KtElement) : KotlinMoveTarget {
    override val targetContainerFqName = targetElement.containingKtFile.packageFqName

    override val targetFile: VirtualFile? = targetElement.containingKtFile.virtualFile

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetElement

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetElement

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

class KotlinMoveTargetForCompanion(val targetClass: KtClass) : KotlinMoveTarget {
    override val targetContainerFqName = targetClass.companionObjects.firstOrNull()?.fqName
        ?: targetClass.fqName!!.child(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)

    override val targetFile: VirtualFile? = targetClass.containingKtFile.virtualFile

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetClass.getOrCreateCompanionObject()

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetClass.companionObjects.firstOrNull()

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

class KotlinMoveTargetForDeferredFile(
    override val targetContainerFqName: FqName,
    override val directory: PsiDirectory?,
    override val targetFile: VirtualFile? = directory?.virtualFile,
    private val createFile: (KtFile) -> KtFile?
) : KotlinDirectoryBasedMoveTarget {
    private val createdFiles = HashMap<KtFile, KtFile?>()

    override fun getOrCreateTargetPsi(originalPsi: PsiElement): KtElement? {
        val originalFile = originalPsi.containingFile as? KtFile ?: return null
        return createdFiles.getOrPutNullable(originalFile) { createFile(originalFile) }
    }

    override fun getTargetPsiIfExists(originalPsi: PsiElement): KtElement? = null

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

class KotlinDirectoryMoveTarget(
    override val targetContainerFqName: FqName,
    override val directory: PsiDirectory
) : KotlinDirectoryBasedMoveTarget {
    override val targetFile: VirtualFile? = directory.virtualFile

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = originalPsi.containingFile as? KtFile

    override fun getTargetPsiIfExists(originalPsi: PsiElement): KtElement? = null

    override fun verify(file: PsiFile): String? = null
}

fun KotlinMoveTarget.getTargetModule(project: Project) = targetScope?.getModule(project)