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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiFile
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.JavaToKotlinTranslator
import org.jetbrains.jet.j2k.ConverterSettings
import org.jetbrains.jet.j2k.PluginSettings
import org.jetbrains.jet.j2k.TestSettings
import java.util.regex.Pattern
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightIdeaTestCase
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.jet.JetTestUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.application.Result
import java.io.BufferedReader
import java.io.StringReader

public abstract class AbstractJavaToKotlinConverterPluginTest() : AbstractJavaToKotlinConverterTest("ide.kt", PluginSettings)
public abstract class AbstractJavaToKotlinConverterBasicTest() : AbstractJavaToKotlinConverterTest("kt", TestSettings)

abstract class AbstractJavaToKotlinConverterTest(val kotlinFileExtension: String, val settings: ConverterSettings ) : LightIdeaTestCase() {

    val testHeaderPattern = Pattern.compile("//(element|expression|statement|method|class|file|comp)\n")

    public fun doTest(javaPath: String) {
        val project = LightPlatformTestCase.getProject()!!
        val converter = Converter(project, settings)
        val javaFile = File(javaPath)
        val fileContents = FileUtil.loadFile(javaFile, true)
        val matcher = testHeaderPattern.matcher(fileContents)
        matcher.find()
        val prefix = matcher.group().trim().substring(2)
        val javaCode = matcher.replaceFirst("")
        val rawConverted = when (prefix) {
            "element" -> elementToKotlin(converter, javaCode)
            "expression" -> expressionToKotlin(converter, javaCode)
            "statement" -> statementToKotlin(converter, javaCode)
            "method" -> methodToKotlin(converter, javaCode)
            "class" -> fileToKotlin(converter, javaCode)
            "file" -> fileToKotlin(converter, javaCode)
            else -> throw IllegalStateException("Specify what is it: file, class, method, statement or expression " +
                                                "using the first line of test data file")
        }

        val reformatInFun = when (prefix) {
            "element", "expression", "statement" -> true
            else -> false
        }

        val actual = reformat(rawConverted, project, reformatInFun)
        val kotlinPath = javaPath.replace(".java", ".$kotlinFileExtension")
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

    private fun elementToKotlin(converter: Converter, text: String): String {
        val fileWithText = JavaToKotlinTranslator.createFile(converter.project, text)!!
        val element = fileWithText.getFirstChild()!!
        return converter.elementToKotlin(element)
    }

    private fun fileToKotlin(converter: Converter, text: String): String {
        return generateKotlinCode(converter, JavaToKotlinTranslator.createFile(converter.project, text))
    }

    private fun methodToKotlin(converter: Converter, text: String?): String {
        var result = fileToKotlin(converter, "final class C {" + text + "}").replaceAll("class C\\(\\) \\{", "")
        result = result.substring(0, (result.lastIndexOf("}"))).trim()
        return result
    }

    private fun statementToKotlin(converter: Converter, text: String?): String {
        var result = methodToKotlin(converter, "void main() {" + text + "}")
        val pos = result.lastIndexOf("}")
        result = result.substring(0, pos).replaceFirst("fun main\\(\\) \\{", "").trim()
        return result
    }

    private fun expressionToKotlin(converter: Converter, code: String?): String {
        var result = statementToKotlin(converter, "final Object o =" + code + "}")
        result = result.replaceFirst("val o : Any\\? =", "").replaceFirst("val o : Any = ", "").replaceFirst("val o = ", "").trim()
        return result
    }

    private fun generateKotlinCode(converter: Converter, file: PsiFile?): String {
        if (file is PsiJavaFile) {
            JavaToKotlinTranslator.setClassIdentifiers(converter, file)
            return converter.elementToKotlin(file)
        }

        return ""
    }

    override fun getProjectJDK(): Sdk? {
        return PluginTestCaseBase.jdkFromIdeaHome()
    }

    private fun String.removeFirstLine(): String {
        val lastNewLine = indexOf('\n')
        return if (lastNewLine == -1) "" else substring(lastNewLine)
    }

    private fun String.removeLastLine(): String {
        val lastNewLine = lastIndexOf('\n')
        return if (lastNewLine == -1) "" else substring(0, lastNewLine)
    }

    private fun String.trimIndent(): String {
        val lines = split('\n')

        val firstNonEmpty = lines.firstOrNull { !it.trim().isEmpty() }
        if (firstNonEmpty == null) {
            return this
        }

        val trimmedPrefix = firstNonEmpty.takeWhile { ch -> ch.isWhitespace() }
        if (trimmedPrefix.isEmpty()) {
            return this
        }

        return lines.map { line ->
            if (line.trim().isEmpty()) {
                ""
            }
            else {
                if (!line.startsWith(trimmedPrefix)) {
                    throw IllegalArgumentException(
                            """Invalid line "$line", ${trimmedPrefix.size} whitespace character are expected""")
                }

                line.substring(trimmedPrefix.length)
            }
        }.makeString(separator = "\n")
    }
}