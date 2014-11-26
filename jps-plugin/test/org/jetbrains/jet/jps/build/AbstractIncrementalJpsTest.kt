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
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import kotlin.test.fail
import java.util.HashMap
import org.jetbrains.jet.utils.keysToMap
import org.jetbrains.jps.incremental.messages.BuildMessage
import kotlin.test.assertFalse
import java.util.regex.Pattern
import kotlin.test.assertEquals
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.jetbrains.jps.cmdline.ProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.jet.utils.Printer

public abstract class AbstractIncrementalJpsTest : JpsBuildTestCase() {
    class object {
        val COMPILATION_FAILED = "COMPILATION FAILED"
    }

    private var testDataDir: File by Delegates.notNull()

    var workDir: File by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")
    }

    override fun tearDown() {
        System.clearProperty("kotlin.jps.tests")
        super.tearDown()
    }

    protected open val customTest: Boolean
        get() = false

    fun build(scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().all()): MakeResult {
        val workDirPath = FileUtil.toSystemIndependentName(workDir.getAbsolutePath())
        val logger = MyLogger(workDirPath)
        val descriptor = createProjectDescriptor(BuildLoggingManager(logger))
        try {
            val buildResult = doBuild(descriptor, scope)!!
            if (!buildResult.isSuccessful()) {
                val errorMessages =
                        buildResult
                                .getMessages(BuildMessage.Kind.ERROR)
                                .joinToString("\n")
                                .replace(File.separatorChar, '/')
                                .replace("/" + workDirPath, "\$PROJECT") // Sometimes path is rendered as "/C:/foo/bar" on Windows
                                .replace(workDirPath, "\$PROJECT")
                return MakeResult(logger.log + "$COMPILATION_FAILED\n" + errorMessages + "\n", true, null)
            }
            else {
                return MakeResult(logger.log, false, createMappingsDump(descriptor))
            }
        } finally {
            descriptor.release()
        }
    }

    private fun initialMake() {
        val makeResult = build()
        assertFalse(makeResult.makeFailed, "Initial make failed:\n$makeResult")
    }

    private fun make(): MakeResult {
        return build()
    }

    private fun rebuild(): MakeResult {
        return build(CompileScopeTestBuilder.rebuild().allModules())
    }

    private fun getModificationsToPerform(moduleNames: Collection<String>?): List<List<Modification>> {

        fun getModificationsForIteration(newSuffix: String, deleteSuffix: String): List<Modification> {

            fun getDirPrefix(fileName: String): String {
                val underscore = fileName.indexOf("_")

                if (underscore != -1) {
                    val module = fileName.substring(0, underscore)

                    assert(moduleNames != null) { "File name has module prefix, but multi-module environment is absent" }
                    assert(module in moduleNames!!) { "Module not found for file with prefix: $fileName" }

                    return module + "/src"
                }

                assert(moduleNames == null) { "Test is multi-module, but file has no module prefix: $fileName" }
                return "src"
            }

            val modifications = ArrayList<Modification>()
            for (file in testDataDir.listFiles()!!) {
                val fileName = file.getName()

                if (fileName.endsWith(newSuffix)) {
                    modifications.add(ModifyContent(getDirPrefix(fileName) + "/" + fileName.trimTrailing(newSuffix), file))
                }
                if (fileName.endsWith(deleteSuffix)) {
                    modifications.add(DeleteFile(getDirPrefix(fileName) + "/" + fileName.trimTrailing(deleteSuffix)))
                }
            }
            return modifications
        }

        val haveFilesWithoutNumbers = testDataDir.listFiles { it.getName().matches(".+\\.(new|delete)$") }?.isNotEmpty() ?: false
        val haveFilesWithNumbers = testDataDir.listFiles { it.getName().matches(".+\\.(new|delete)\\.\\d+$") }?.isNotEmpty() ?: false

        if (haveFilesWithoutNumbers && haveFilesWithNumbers) {
            fail("Bad test data format: files ending with both unnumbered and numbered \".new\"/\".delete\" were found")
        }
        if (!haveFilesWithoutNumbers && !haveFilesWithNumbers) {
            if (customTest) {
                return listOf(listOf())
            }
            else {
                fail("Bad test data format: no files ending with \".new\" or \".delete\" found")
            }
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

    private fun rebuildAndCheckOutput(makeOverallResult: MakeResult) {
        val outDir = File(getAbsolutePath("out"))
        val outAfterMake = File(getAbsolutePath("out-after-make"))
        FileUtil.copyDir(outDir, outAfterMake)

        val rebuildResult = rebuild()
        assertEquals(rebuildResult.makeFailed, makeOverallResult.makeFailed,
                     "Rebuild failed: ${rebuildResult.makeFailed}, last make failed: ${makeOverallResult.makeFailed}. Rebuild log: $rebuildResult")

        assertEqualDirectories(outDir, outAfterMake, makeOverallResult.makeFailed)

        if (!makeOverallResult.makeFailed) {
            TestCase.assertEquals(rebuildResult.mappingsDump, makeOverallResult.mappingsDump)
        }

        FileUtil.delete(outAfterMake)
    }

    private fun clearCachesRebuildAndCheckOutput(makeOverallResult: MakeResult) {
        FileUtil.delete(BuildDataPathsImpl(myDataStorageRoot).getDataStorageRoot()!!)

        rebuildAndCheckOutput(makeOverallResult)
    }

    private fun readModuleDependencies(): Map<String, List<String>>? {
        val dependenciesTxt = File(testDataDir, "dependencies.txt")
        if (!dependenciesTxt.exists()) return null

        val result = HashMap<String, List<String>>()
        for (line in dependenciesTxt.readLines()) {
            val split = line.split("->")
            val module = split[0]
            val dependencies = split[1].split(",")

            result[module] = dependencies.toList()
        }

        return result
    }

    protected fun doTest(testDataPath: String) {
        if (!IncrementalCompilation.ENABLED) {
            return
        }

        testDataDir = File(testDataPath)
        workDir = FileUtil.createTempDirectory("jps-build", null)

        val moduleNames = configureModules()
        initialMake()

        val makeOverallResult = performModificationsAndMake(moduleNames)
        UsefulTestCase.assertSameLinesWithFile(File(testDataDir, "build.log").getAbsolutePath(), makeOverallResult.log)

        rebuildAndCheckOutput(makeOverallResult)
        clearCachesRebuildAndCheckOutput(makeOverallResult)
    }

    private fun createMappingsDump(project: ProjectDescriptor) =
            createCommonMappingsDump(project) + "\n\n\n" + createJavaMappingsDump(project)

    private fun createJavaMappingsDump(project: ProjectDescriptor): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        PrintStream(byteArrayOutputStream).use {
            project.dataManager.getMappings().toStream(it)
        }
        return byteArrayOutputStream.toString()
    }

    private fun createCommonMappingsDump(project: ProjectDescriptor): String {
        val resultBuf = StringBuilder()
        val result = Printer(resultBuf)

        result.println("Begin of SourceToOutputMap")
        result.pushIndent()

        for (target in project.getBuildTargetIndex().getAllTargets()) {
            result.println(target)
            result.pushIndent()

            val mapping = project.dataManager.getSourceToOutputMap(target)
            mapping.getSources().forEach {
                result.println("source $it -> " + mapping.getOutputs(it).sort() )
            }

            result.popIndent()
        }

        result.popIndent()
        result.println("End of SourceToOutputMap")

        return resultBuf.toString()
    }

    private data class MakeResult(val log: String, val makeFailed: Boolean, val mappingsDump: String?)

    private fun performModificationsAndMake(moduleNames: Set<String>?): MakeResult {
        val logs = ArrayList<String>()

        val modifications = getModificationsToPerform(moduleNames)
        var lastCompilationFailed = false
        var lastMappingsDump: String? = null
        for (step in modifications) {
            step.forEach { it.perform(workDir) }
            performAdditionalModifications()

            val makeResult = make()
            logs.add(makeResult.log)
            lastCompilationFailed = makeResult.makeFailed
            lastMappingsDump = makeResult.mappingsDump
        }

        return MakeResult(logs.join("\n\n"), lastCompilationFailed, lastMappingsDump)
    }

    protected open fun performAdditionalModifications() {
    }

    // null means one module
    private fun configureModules(): Set<String>? {
        var moduleNames: Set<String>?
        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject)
                .setOutputUrl(JpsPathUtil.pathToUrl(getAbsolutePath("out")))

        val jdk = addJdk("my jdk")
        val moduleDependencies = readModuleDependencies()
        if (moduleDependencies == null) {
            addModule("module", array(getAbsolutePath("src")), null, null, jdk)

            FileUtil.copyDir(testDataDir, File(workDir, "src"), { it.getName().endsWith(".kt") || it.getName().endsWith(".java") })

            moduleNames = null
        }
        else {
            val nameToModule = moduleDependencies.keySet()
                    .keysToMap { addModule(it, array(getAbsolutePath(it + "/src")), null, null, jdk)!! }

            for ((moduleName, dependencies) in moduleDependencies) {
                val module = nameToModule[moduleName]!!
                for (dependency in dependencies) {
                    module.getDependenciesList().addModuleDependency(nameToModule[dependency]!!)
                }
            }

            for (module in nameToModule.values()) {
                val moduleName = module.getName()

                FileUtil.copyDir(testDataDir, File(workDir, moduleName + "/src"),
                                 { it.getName().startsWith(moduleName + "_") && (it.getName().endsWith(".kt") || it.getName().endsWith(".java")) })
            }

            moduleNames = nameToModule.keySet()
        }
        AbstractKotlinJpsBuildTestCase.addKotlinRuntimeDependency(myProject)
        return moduleNames
    }

    override fun doGetProjectDir(): File? = workDir

    private class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase() {
        private val logBuf = StringBuilder()
        public val log: String
            get() = logBuf.toString()

        override fun isEnabled(): Boolean = true

        override fun logLine(message: String?) {
            fun String.replaceHashWithStar(): String {
                val matcher = STRIP_PACKAGE_PART_HASH_PATTERN.matcher(this)
                if (matcher.find()) {
                    return matcher.replaceAll("\\$*")
                }
                return this
            }

            logBuf.append(message!!.trimLeading(rootPath + "/").replaceHashWithStar()).append('\n')
        }

        class object {
            // We suspect sequences of eight consecutive hexadecimal digits to be a package part hash code
            val STRIP_PACKAGE_PART_HASH_PATTERN = Pattern.compile("\\$([0-9a-f]{8})")
        }
    }

    private abstract class Modification(val path: String) {
        abstract fun perform(workDir: File)

        override fun toString(): String = "${javaClass.getSimpleName()} $path"
    }

    private class ModifyContent(path: String, val dataFile: File) : Modification(path) {
        override fun perform(workDir: File) {
            val file = File(workDir, path)

            val oldLastModified = file.lastModified()
            dataFile.copyTo(file)

            val newLastModified = file.lastModified()
            if (newLastModified <= oldLastModified) {
                //Mac OS and some versions of Linux truncate timestamp to nearest second
                file.setLastModified(oldLastModified + 1000)
            }
        }
    }

    private class DeleteFile(path: String) : Modification(path) {
        override fun perform(workDir: File) {
            val fileToDelete = File(workDir, path)
            if (!fileToDelete.delete()) {
                throw AssertionError("Couldn't delete $fileToDelete")
            }
        }
    }
}
