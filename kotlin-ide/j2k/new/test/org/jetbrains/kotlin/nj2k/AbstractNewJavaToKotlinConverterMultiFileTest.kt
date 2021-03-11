/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterTest
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNewJavaToKotlinConverterMultiFileTest : AbstractJavaToKotlinConverterTest() {
    fun doTest(dirPath: String) {
        val psiManager = PsiManager.getInstance(project)
        val filesToConvert = File(dirPath).listFiles { _, name -> name.endsWith(".java") }!!
        val psiFilesToConvert = filesToConvert.map { javaFile ->
            val virtualFile = addFile(javaFile, "test")
            psiManager.findFile(virtualFile) as PsiJavaFile
        }

        val externalFiles = File(dirPath + File.separator + "external")
            .takeIf { it.exists() }
            ?.listFiles { _, name ->
                name.endsWith(".java") || name.endsWith(".kt")
            }.orEmpty()

        val externalPsiFiles = externalFiles.map { file ->
            val virtualFile = addFile(file, "test")
            val psiFile = psiManager.findFile(virtualFile)!!
            assert(psiFile is PsiJavaFile || psiFile is KtFile)
            psiFile
        }

        val converter = NewJavaToKotlinConverter(project, module, ConverterSettings.defaultSettings, IdeaJavaToKotlinServices)
        val (results, externalCodeProcessor) =
            WriteCommandAction.runWriteCommandAction(project, Computable {
                PostprocessReformattingAspect.getInstance(project).doPostponedFormatting()
                return@Computable converter.filesToKotlin(psiFilesToConvert, NewJ2kPostProcessor())
            })
        val process = externalCodeProcessor?.prepareWriteOperation(progress = null)

        fun expectedResultFile(i: Int) = File(filesToConvert[i].path.replace(".java", ".kt"))

        val resultFiles = psiFilesToConvert.mapIndexed { i, javaFile ->
            deleteFile(javaFile.virtualFile)
            val virtualFile = addFile(results[i], expectedResultFile(i).name, "test")
            psiManager.findFile(virtualFile) as KtFile
        }

        resultFiles.forEach { it.commitAndUnblockDocument() }

        project.executeWriteCommand("") {
            process?.invoke(resultFiles)
        }

        for ((i, kotlinFile) in resultFiles.withIndex()) {
            KotlinTestUtils.assertEqualsToFile(expectedResultFile(i), kotlinFile.dumpTextWithErrors())
        }

        for ((externalFile, externalPsiFile) in externalFiles.zip(externalPsiFiles)) {
            val expectedFile = File(externalFile.path + ".expected")
            val resultText = when (externalPsiFile) {
                is KtFile -> externalPsiFile.dumpTextWithErrors()
                else -> externalPsiFile.text
            }
            KotlinTestUtils.assertEqualsToFile(expectedFile, resultText)
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
