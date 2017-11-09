/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

abstract class AbstractJavaToKotlinConverterMultiFileTest : AbstractJavaToKotlinConverterTest() {
    fun doTest(dirPath: String) {
        val project = LightPlatformTestCase.getProject()!!
        val psiManager = PsiManager.getInstance(project)

        val filesToConvert = File(dirPath).listFiles { _, name -> name.endsWith(".java") }
        val psiFilesToConvert = ArrayList<PsiJavaFile>()
        for (javaFile in filesToConvert) {
            val virtualFile = addFile(javaFile, "test")
            val psiFile = psiManager.findFile(virtualFile) as PsiJavaFile
            psiFilesToConvert.add(psiFile)
        }

        val externalFiles = File(dirPath + File.separator + "external").listFiles { _, name -> name.endsWith(".java") || name.endsWith(".kt") }
        val externalPsiFiles = ArrayList<PsiFile>()
        for (file in externalFiles) {
            val virtualFile = addFile(file, "test")
            val psiFile = psiManager.findFile(virtualFile)!!
            externalPsiFiles.add(psiFile)
            assert(psiFile is PsiJavaFile || psiFile is KtFile)
        }

        val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, IdeaJavaToKotlinServices)
        val (results, externalCodeProcessor) = converter.filesToKotlin(psiFilesToConvert, J2kPostProcessor(formatCode = true))

        val process = externalCodeProcessor?.prepareWriteOperation(EmptyProgressIndicator())
        project.executeWriteCommand("") { process?.invoke() }

        fun expectedResultFile(i: Int) = File(filesToConvert[i].path.replace(".java", ".kt"))

        val resultFiles = ArrayList<KtFile>()
        for ((i, javaFile) in psiFilesToConvert.withIndex()) {
            deleteFile(javaFile.virtualFile)
            val virtualFile = addFile(results[i], expectedResultFile(i).name, "test")
            resultFiles.add(psiManager.findFile(virtualFile) as KtFile)
        }

        for ((i, kotlinFile) in resultFiles.withIndex()) {
            KotlinTestUtils.assertEqualsToFile(expectedResultFile(i), kotlinFile.dumpTextWithErrors())
        }

        for ((externalFile, externalPsiFile) in externalFiles.zip(externalPsiFiles)) {
            val expectedFile = File(externalFile.path + ".expected")
            var resultText = if (externalPsiFile is KtFile) {
                externalPsiFile.dumpTextWithErrors()
            }
            else {
                //TODO: errors dump for java files too
                externalPsiFile.text
            }
            KotlinTestUtils.assertEqualsToFile(expectedFile, resultText)
        }
    }

    override fun getProjectDescriptor()
            = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
