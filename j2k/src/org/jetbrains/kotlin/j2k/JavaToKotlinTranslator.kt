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

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile

object JavaToKotlinTranslator {
    private fun createFile(text: String, project: Project): PsiFile? {
        return PsiFileFactory.getInstance(project).createFileFromText("test.java", JavaLanguage.INSTANCE, text)
    }

    private fun prettify(code: String?): String {
        if (code == null) {
            return ""
        }

        return code
                .trim()
                .replace("\r\n", "\n")
                .replace(" \n", "\n")
                .replace("\n ", "\n")
                .replace("\n+".toRegex(), "\n")
                .replace(" +".toRegex(), " ")
                .trim()
    }

    fun generateKotlinCode(javaCode: String, project: Project): String {
        val file = createFile(javaCode, project)
        if (file is PsiJavaFile) {
            val converter = JavaToKotlinConverter(file.project, ConverterSettings.defaultSettings, EmptyJavaToKotlinServices)
            return prettify(converter.elementsToKotlin(listOf(file)).results.single()!!.text) //TODO: imports
        }
        return ""
    }
}

//used in Kotlin Web Demo
fun translateToKotlin(code: String, project: Project): String {
    return JavaToKotlinTranslator.generateKotlinCode(code, project)
}
