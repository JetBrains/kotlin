/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.stubs.KaptLineMappingCollectorBase
import java.io.File

internal class Kapt4LineMappingCollector : KaptLineMappingCollectorBase() {
    private val filePaths = mutableMapOf<PsiFile, Pair<String, Boolean>>()

    fun registerClass(lightClass: PsiClass) {
        register(lightClass, lightClass.qualifiedNameWithSlashes)
    }

    fun registerMethod(lightClass: PsiClass, method: PsiMethod) {
        register(method, lightClass.qualifiedNameWithSlashes + "#" + method.name + method.signature)
    }

    fun registerField(lightClass: PsiClass, field: PsiField) {
        register(field, lightClass.qualifiedNameWithSlashes + "#" + field.name)
    }

    fun getPosition(clazz: PsiClass): KotlinPosition? =
        lineInfo[clazz.qualifiedNameWithSlashes]

    fun getPosition(lightClass: PsiClass, method: PsiMethod): KotlinPosition? =
        lineInfo[lightClass.qualifiedNameWithSlashes + "#" + method.name + method.signature]

    fun getPosition(lightClass: PsiClass, field: PsiField): KotlinPosition? {
        return lineInfo[lightClass.qualifiedNameWithSlashes + "#" + field.name]
    }

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = (asmNode as? KtLightElement<*, *>)?.kotlinOrigin ?: return
        val containingVirtualFile = psiElement.containingFile.virtualFile
        if (containingVirtualFile == null || FileDocumentManager.getInstance().getDocument(containingVirtualFile) == null) {
            return
        }

        val textRange = psiElement.textRange ?: return

        val (path, isRelative) = getFilePathRelativePreferred(psiElement.containingFile)
        lineInfo[fqName] = KotlinPosition(path, isRelative, textRange.startOffset)
    }

    private fun getFilePathRelativePreferred(file: PsiFile): Pair<String, Boolean> {
        return filePaths.getOrPut(file) {
            val absolutePath = file.virtualFile.canonicalPath ?: file.virtualFile.path
            val absoluteFile = File(absolutePath)
            val baseFile = file.project.basePath?.let { File(it) }

            if (absoluteFile.exists() && baseFile != null && baseFile.exists()) {
                val relativePath = absoluteFile.relativeToOrNull(baseFile)?.path
                if (relativePath != null) {
                    return@getOrPut Pair(relativePath, true)
                }
            }

            return@getOrPut Pair(absolutePath, false)
        }
    }

    private val PsiClass.qualifiedNameWithSlashes: String
        get() = qualifiedNameWithDollars?.replace(".", "/") ?: "<no name provided>"

    fun registerSignature(javacSignature: String, method: PsiMethod) {
        signatureInfo[javacSignature] = method.name + method.signature
    }
}
