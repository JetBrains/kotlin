/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.test

import java.io.File
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.jet.JetTestUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.jet.j2k.FilesConversionScope
import org.jetbrains.jet.plugin.j2k.J2kPostProcessor
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.psi.PsiJavaFile
import org.jetbrains.jet.j2k.IdeaReferenceSearcher
import org.jetbrains.jet.j2k.JavaToKotlinConverter
import com.intellij.psi.PsiManager
import java.util.ArrayList
import org.jetbrains.jet.j2k.ConverterSettings

public abstract class AbstractJavaToKotlinConverterMultiFileTest() : AbstractJavaToKotlinConverterTest() {
    public fun doTest(dirPath: String) {
        val project = LightPlatformTestCase.getProject()!!

        val javaFiles = File(dirPath).listFiles {(file, name): Boolean -> name.endsWith(".java") }
        val psiFiles = ArrayList<PsiJavaFile>()
        for (javaFile: File in javaFiles) {
            val virtualFile = addFile(javaFile, "test")
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as PsiJavaFile
            psiFiles.add(psiFile)
        }

        val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, FilesConversionScope(psiFiles), IdeaReferenceSearcher)
        val results: List<String> = converter.elementsToKotlin(psiFiles.map { it to J2kPostProcessor(it) })
                .map { reformat(it, project) }

        for ((i, javaFile) in javaFiles.withIndices()) {
            val kotlinPath = javaFile.getPath().replace(".java", ".kt")
            val expectedFile = File(kotlinPath)
            JetTestUtils.assertEqualsToFile(expectedFile, results[i])
        }
    }

    private fun reformat(text: String, project: Project): String {
        val convertedFile = JetTestUtils.createFile("converted", text, project)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project)!!.reformat(convertedFile)
        }
        return convertedFile.getText()!!
    }

    override fun getProjectDescriptor()
            = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
