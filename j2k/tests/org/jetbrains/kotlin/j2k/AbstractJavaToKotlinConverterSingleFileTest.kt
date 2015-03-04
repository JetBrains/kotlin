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
import com.intellij.openapi.util.io.FileUtil
import java.util.regex.Pattern
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.test.JetTestUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.kotlin.test.util.trimIndent
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter

public abstract class AbstractJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterTest() {
    val testHeaderPattern = Pattern.compile("//(element|expression|statement|method|class|file|comp)\n")

    public fun doTest(javaPath: String) {
        val project = LightPlatformTestCase.getProject()!!
        val javaFile = File(javaPath)
        val fileContents = FileUtil.loadFile(javaFile, true)
        val matcher = testHeaderPattern.matcher(fileContents)

        val (prefix, javaCode) = if (matcher.find()) {
            Pair(matcher.group().trim().substring(2), matcher.replaceFirst(""))
        }
        else {
            Pair("file", fileContents)
        }

        fun parseBoolean(text: String): Boolean = when (text) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Unknown option value: $text")
        }

        var settings = ConverterSettings.defaultSettings.copy()
        val directives = JetTestUtils.parseDirectives(javaCode)
        for ((name, value) in directives) {
            when (name) {
                "forceNotNullTypes" -> settings.forceNotNullTypes = parseBoolean(value)
                "specifyLocalVariableTypeByDefault" -> settings.specifyLocalVariableTypeByDefault = parseBoolean(value)
                "specifyFieldTypeByDefault" -> settings.specifyFieldTypeByDefault = parseBoolean(value)
                "openByDefault" -> settings.openByDefault = parseBoolean(value)
                else -> throw IllegalArgumentException("Unknown option: $name")
            }
        }

        val rawConverted = when (prefix) {
            "element" -> elementToKotlin(javaCode, settings, project)
            "expression" -> expressionToKotlin(javaCode, settings, project)
            "statement" -> statementToKotlin(javaCode, settings, project)
            "method" -> methodToKotlin(javaCode, settings, project)
            "class" -> fileToKotlin(javaCode, settings, project)
            "file" -> fileToKotlin(javaCode, settings, project)
            else -> throw IllegalStateException("Specify what is it: file, class, method, statement or expression " +
                                                "using the first line of test data file")
        }

        val reformatInFun = prefix in setOf("element", "expression", "statement")

        var actual = reformat(rawConverted, project, reformatInFun)

        if (prefix == "file") {
            actual = addErrorsDump(createKotlinFile(actual))
        }

        val kotlinPath = javaPath.replace(".java", ".kt")
        val expectedFile = File(kotlinPath)
        JetTestUtils.assertEqualsToFile(expectedFile, actual)
    }

    private fun reformat(text: String, project: Project, inFunContext: Boolean): String {
        val textToFormat = if (inFunContext) "fun convertedTemp() {\n$text\n}" else text

        val convertedFile = JetTestUtils.createFile("converted", textToFormat, project)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project)!!.reformat(convertedFile)
        }

        val reformattedText = convertedFile.getText()!!

        return if (inFunContext)
            reformattedText.removeFirstLine().removeLastLine().trimIndent()
        else
            reformattedText
    }

    private fun elementToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val fileWithText = createJavaFile(text)
        val converter = JavaToKotlinConverter(project, settings, FilesConversionScope(listOf(fileWithText)), IdeaReferenceSearcher, IdeaResolverForConverter)
        val element = fileWithText.getFirstChild()!!
        return converter.elementsToKotlin(listOf(element to J2kPostProcessor(fileWithText)))[0]
    }

    private fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        val converter = JavaToKotlinConverter(project, settings, FilesConversionScope(listOf(file)), IdeaReferenceSearcher, IdeaResolverForConverter)
        return converter.elementsToKotlin(listOf(file to J2kPostProcessor(file)))[0]
    }

    private fun methodToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val result = fileToKotlin("final class C {" + text + "}", settings, project).replaceAll("class C \\{", "")
        return result.substring(0, (result.lastIndexOf("}"))).trim()
    }

    private fun statementToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val result = methodToKotlin("void main() {" + text + "}", settings, project)
        return result.substring(0, result.lastIndexOf("}")).replaceFirst("fun main\\(\\) \\{", "").trim()
    }

    private fun expressionToKotlin(code: String, settings: ConverterSettings, project: Project): String {
        val result = statementToKotlin("final Object o =" + code + "}", settings, project)
        return result.replaceFirst("val o:Any\\? = ", "").replaceFirst("val o:Any = ", "").replaceFirst("val o = ", "").trim()
    }

    override fun getProjectDescriptor()
            = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private fun String.removeFirstLine() = substringAfter('\n', "")

    private fun String.removeLastLine() = substringBeforeLast('\n', "")

    private fun createJavaFile(text: String): PsiJavaFile {
        return myFixture.configureByText("converterTestFile.java", text) as PsiJavaFile
    }

    private fun createKotlinFile(text: String): JetFile {
        return myFixture.configureByText("converterTestFile.kt", text) as JetFile
    }
}
