/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.checkers.AbstractPsiCheckerTest
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager.Companion.updateScriptDependenciesSynchronously
import org.jetbrains.kotlin.idea.navigation.GotoCheck
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File
import java.util.regex.Pattern


abstract class AbstractScriptConfigurationHighlightingTest : AbstractScriptConfigurationTest() {
    override fun doTest(path: String) {
        configureScriptEnvironment(path)

        super.doTest(setupScriptFile(path))
    }
}

abstract class AbstractScriptConfigurationNavigationTest : AbstractScriptConfigurationTest() {

    override fun doTest(path: String) {
        configureScriptEnvironment(path)

        myFixture.configureFromExistingVirtualFile(setupScriptFile(path))
        val reference = myFixture.getReferenceAtCaretPosition()!!

        val resolved = reference.resolve()!!.navigationElement!!

        val expectedReference = InTextDirectivesUtils.findStringWithPrefixes(myFixture.file.text, "// REF:")
        val actualReference = resolved.renderAsGotoImplementation()

        Assert.assertEquals(expectedReference, actualReference)

        val expectedFile = InTextDirectivesUtils.findStringWithPrefixes(myFixture.file.text, "// FILE:")
        val actualFile = GotoCheck.getFileWithDir(resolved)

        Assert.assertEquals(expectedFile, actualFile)
    }
}

abstract class AbstractScriptConfigurationTest : AbstractPsiCheckerTest() {
    protected fun configureScriptEnvironment(path: String) {
        val templateOutDir = compileLibToDir(
                File("${path}template"),
                PathUtil.getKotlinPathsForDistDirectory().scriptRuntimePath.path
        )

        val libSrcDir = File("${path}lib")
        val libClasses = if (libSrcDir.isDirectory) {
            compileLibToDir(libSrcDir)
        }
        else null

        registerScriptTemplateProvider(templateOutDir, libClasses, libSrcDir)
    }

    protected fun setupScriptFile(path: String): VirtualFile {
        val scriptDir = KotlinTestUtils.tmpDir("scriptDir")
        val target = File(scriptDir, "script.kts")
        File("${path}script.kts").copyTo(target)
        val scriptFile = LocalFileSystem.getInstance().findFileByPath(target.path)!!
        updateScriptDependenciesSynchronously(scriptFile, project)
        return scriptFile
    }

    private fun compileLibToDir(srcDir: File, vararg classpath: String): File {
        val outDir = KotlinTestUtils.tmpDir("${getTestName(false)}${srcDir.name}Out")

        val kotlinSourceFiles = FileUtil.findFilesByMask(Pattern.compile(".+\\.kt$"), srcDir)
        if (kotlinSourceFiles.isNotEmpty()) {
            MockLibraryUtil.compileKotlin(srcDir.path, outDir, extraClasspath = *classpath)
        }

        val javaSourceFiles = FileUtil.findFilesByMask(Pattern.compile(".+\\.java$"), srcDir)
        if (javaSourceFiles.isNotEmpty()) {
            KotlinTestUtils.compileJavaFiles(
                    javaSourceFiles,
                    listOf("-cp", StringUtil.join(listOf(*classpath, outDir), File.pathSeparator), "-d", outDir.path)
            )
        }
        return outDir
    }

    private fun registerScriptTemplateProvider(templateDir: File, libClasses: File?, libSource: File?) {
        val provider = TestScriptTemplateProvider(
                templateDir,
                mapOf(
                        "runtime-classes" to ForTestCompileRuntime.runtimeJarForTests(),
                        "runtime-source" to File("libraries/stdlib/src"),
                        "lib-classes" to libClasses,
                        "lib-source" to libSource,
                        "template-classes" to templateDir
                )
        )

        PlatformTestUtil.registerExtension(
                Extensions.getArea(project),
                ScriptTemplatesProvider.EP_NAME,
                provider,
                myTestRootDisposable
        )
        ScriptDependenciesManager.reloadScriptDefinitions(project)
    }

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE
}

class TestScriptTemplateProvider(
        compiledTemplateDir: File,
        override val environment: Map<String, Any?>
) : ScriptTemplatesProvider {
    override val id = "Test"
    override val isValid = true
    override val templateClassNames = listOf("custom.scriptDefinition.Template")
    override val templateClasspath = listOf(compiledTemplateDir)
}
