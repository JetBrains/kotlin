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

package org.jetbrains.jet.jps.build

import org.jetbrains.jps.builders.JpsBuildTestCase
import kotlin.properties.Delegates
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jps.builders.CompileScopeTestBuilder
import org.jetbrains.jps.builders.impl.logging.ProjectBuilderLoggerBase
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.util.JpsPathUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.config.IncrementalCompilation
import java.util.ArrayList
import kotlin.test.fail

public class IncrementalJpsTest : JpsBuildTestCase() {
    private val testDataDir: File
        get() = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "incremental/" + getTestName(true))

    var workDir: File by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")

        workDir = FileUtil.createTempDirectory("jps-build", null)

        FileUtil.copyDir(testDataDir, File(workDir, "src"), { it.getName().endsWith(".kt") || it.getName().endsWith(".java") })

        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject!!)
                .setOutputUrl(JpsPathUtil.pathToUrl(getAbsolutePath("out")))
    }

    override fun tearDown() {
        System.clearProperty("kotlin.jps.tests")
        super.tearDown()
    }

    fun buildGetLog(scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().all()): String {
        val logger = MyLogger(FileUtil.toSystemIndependentName(workDir.getAbsolutePath()))
        val descriptor = createProjectDescriptor(BuildLoggingManager(logger))
        try {
            doBuild(descriptor, scope)!!.assertSuccessful()
            return logger.log
        } finally {
            descriptor.release()
        }
    }

    private fun getModificationsToPerform(): List<List<Modification>> {

        fun getModificationsForIteration(newSuffix: String, deleteSuffix: String): List<Modification> {
            val modifications = ArrayList<Modification>()
            for (file in testDataDir.listFiles()!!) {
                if (file.getName().endsWith(newSuffix)) {
                    modifications.add(ModifyContent(file.getName().trimTrailing(newSuffix), file))
                }
                if (file.getName().endsWith(deleteSuffix)) {
                    modifications.add(DeleteFile(file.getName().trimTrailing(deleteSuffix)))
                }
            }
            return modifications
        }

        val haveFilesWithoutNumbers = testDataDir.listFiles { it.getName().matches(".+\\.(new|delete)$") }?.isNotEmpty() ?: false
        val haveFilesWithNumbers = testDataDir.listFiles { it.getName().matches(".+\\.(new|delete)\\.\\d+$") }?.isNotEmpty() ?: false

        if (haveFilesWithoutNumbers && haveFilesWithNumbers) {
            fail("Bad test data format: no files ending with \".new\" or \".delete\" found")
        }
        if (!haveFilesWithoutNumbers && !haveFilesWithNumbers) {
            fail("Bad test data format: files ending with both unnumbered and numbered \".new\"/\".delete\" were found")
        }

        if (haveFilesWithoutNumbers) {
            return listOf(getModificationsForIteration(".new", ".delete"))
        }
        else {
            return (1..10)
                    .map { getModificationsForIteration(".new.$it", ".delete.$it") }
                    .filter { it.isNotEmpty() }
        }
    }

    private fun doTest() {
        if (!IncrementalCompilation.ENABLED) {
            return
        }

        addModule("module", array(getAbsolutePath("src")), null, null, addJdk("my jdk"))
        AbstractKotlinJpsBuildTestCase.addKotlinRuntimeDependency(myProject!!)

        buildGetLog()

        val modifications = getModificationsToPerform()
        val logs = ArrayList<String>()

        for (step in modifications) {
            step.forEach { it.perform(workDir) }

            val log = buildGetLog()
            logs.add(log)
        }

        UsefulTestCase.assertSameLinesWithFile(File(testDataDir, "build.log").getAbsolutePath(), logs.makeString("\n\n"))

        val outDir = File(getAbsolutePath("out"))
        val outAfterMake = File(getAbsolutePath("out-after-make"))
        FileUtil.copyDir(outDir, outAfterMake)

        buildGetLog(CompileScopeTestBuilder.rebuild().allModules())

        assertEqualDirectories(outDir, outAfterMake, { it.name == "script.xml" })
    }

    override fun doGetProjectDir(): File? = workDir

    fun testIndependentClasses() {
        doTest()
    }

    fun testSimpleClassDependency() {
        doTest()
    }

    fun testTopLevelMembersInTwoPackages() {
        doTest()
    }

    fun testReturnTypeChanged() {
        doTest()
    }

    fun testTopLevelFunctionSameSignature() {
        doTest()
    }

    fun testClassSignatureChanged() {
        doTest()
    }

    fun testConstantValue() {
        doTest()
    }

    fun testConstantUnchanged() {
        doTest()
    }

    private class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase() {
        private val logBuf = StringBuilder()
        public val log: String
            get() = logBuf.toString()

        override fun isEnabled(): Boolean = true

        override fun logLine(message: String?) {

            fun String.replaceHashWithStar(): String {
                val lastHyphen = this.lastIndexOf('-')
                if (lastHyphen != -1 && substring(lastHyphen + 1).matches("[0-9a-f]{1,8}\\.class")) {
                    return substring(0, lastHyphen) + "-*.class"
                }
                return this
            }

            logBuf.append(message!!.trimLeading(rootPath + "/").replaceHashWithStar()).append('\n')
        }
    }

    private abstract class Modification(val name: String) {
        abstract fun perform(workDir: File)

        override fun toString(): String = "${javaClass.getSimpleName()} $name"
    }

    private class ModifyContent(name: String, val dataFile: File) : Modification(name) {
        override fun perform(workDir: File) {
            dataFile.copyTo(File(workDir, "src/$name"))
        }
    }

    private class DeleteFile(name: String) : Modification(name) {
        override fun perform(workDir: File) {
            val fileToDelete = File(workDir, "src/$name")
            if (!fileToDelete.delete()) {
                throw AssertionError("Couldn't delete $fileToDelete")
            }
        }
    }
}
