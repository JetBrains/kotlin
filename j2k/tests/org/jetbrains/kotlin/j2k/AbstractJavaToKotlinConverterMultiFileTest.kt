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

import java.io.File
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.test.JetTestUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter

public abstract class AbstractJavaToKotlinConverterMultiFileTest : AbstractJavaToKotlinConverterTest() {
    public fun doTest(dirPath: String) {
        val project = LightPlatformTestCase.getProject()!!
        val psiManager = PsiManager.getInstance(project)

        val javaFiles = File(dirPath).listFiles { file, name -> name.endsWith(".java") }
        val psiFiles = ArrayList<PsiJavaFile>()
        for (javaFile: File in javaFiles) {
            val virtualFile = addFile(javaFile, "test")
            val psiFile = psiManager.findFile(virtualFile) as PsiJavaFile
            psiFiles.add(psiFile)
        }

        val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings,
                                              IdeaReferenceSearcher, IdeaResolverForConverter, J2kPostProcessor(formatCode = true))
        val inputElements = psiFiles.map { JavaToKotlinConverter.InputElement(it, it) }
        val results: List<String> = converter.elementsToKotlin(inputElements)

        fun expectedFile(i: Int) = File(javaFiles[i].getPath().replace(".java", ".kt"))

        val jetFiles = ArrayList<JetFile>()
        for (i in javaFiles.indices) {
            deleteFile(psiFiles[i].getVirtualFile())
            val virtualFile = addFile(results[i], expectedFile(i).getName(), "test")
            jetFiles.add(psiManager.findFile(virtualFile) as JetFile)
        }

        for ((i, jetFile) in jetFiles.withIndex()) {
            JetTestUtils.assertEqualsToFile(expectedFile(i), addErrorsDump(jetFile))
        }
    }

    override fun getProjectDescriptor()
            = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
