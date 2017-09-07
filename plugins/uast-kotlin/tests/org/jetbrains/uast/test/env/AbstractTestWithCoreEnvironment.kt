/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.uast.test.env

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiManager
import com.intellij.rt.execution.junit.FileComparisonFailure
import junit.framework.TestCase
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.java.JavaUastLanguagePlugin
import java.io.File

abstract class AbstractTestWithCoreEnvironment : TestCase() {
    private var myEnvironment: AbstractCoreEnvironment? = null

    protected val environment: AbstractCoreEnvironment
        get() = myEnvironment!!

    protected lateinit var project: MockProject

    protected val uastContext: UastContext by lazy {
        ServiceManager.getService(project, UastContext::class.java)
    }

    protected val psiManager: PsiManager by lazy {
        PsiManager.getInstance(project)
    }

    override fun tearDown() {
        disposeEnvironment()
    }

    protected abstract fun createEnvironment(source: File): AbstractCoreEnvironment

    protected fun initializeEnvironment(source: File) {
        if (myEnvironment != null) {
            error("Environment is already initialized")
        }
        myEnvironment = createEnvironment(source)
        project = environment.project

        CoreApplicationEnvironment.registerExtensionPoint(
                Extensions.getRootArea(),
                UastLanguagePlugin.extensionPointName,
                UastLanguagePlugin::class.java)

        CoreApplicationEnvironment.registerExtensionPoint(
                Extensions.getRootArea(),
                UEvaluatorExtension.EXTENSION_POINT_NAME,
                UEvaluatorExtension::class.java)

        project.registerService(UastContext::class.java, UastContext::class.java)

        registerUastLanguagePlugins()
    }

    private fun registerUastLanguagePlugins() {
        val area = Extensions.getRootArea()

        area.getExtensionPoint(UastLanguagePlugin.extensionPointName)
                .registerExtension(JavaUastLanguagePlugin())
    }

    protected fun disposeEnvironment() {
        myEnvironment?.dispose()
        myEnvironment = null
    }
}

private fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
        this.split('\n').map(String::trimEnd).joinToString(separator = "\n").let {
            result -> if (result.endsWith("\n")) result else result + "\n"
        }

fun assertEqualsToFile(description: String, expected: File, actual: String) {
    if (!expected.exists()) {
        expected.writeText(actual)
        TestCase.fail("File didn't exist. New file was created (${expected.canonicalPath}).")
    }

    val expectedText =
            StringUtil.convertLineSeparators(expected.readText().trim()).trimTrailingWhitespacesAndAddNewlineAtEOF()
    val actualText =
            StringUtil.convertLineSeparators(actual.trim()).trimTrailingWhitespacesAndAddNewlineAtEOF()
    if (expectedText != actualText) {
        throw FileComparisonFailure(description, expectedText, actualText, expected.absolutePath)
    }
}
