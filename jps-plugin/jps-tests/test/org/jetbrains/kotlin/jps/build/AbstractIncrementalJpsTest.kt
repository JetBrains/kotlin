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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.testFramework.TestLoggerFactory
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.concurrency.FixedFuture
import junit.framework.TestCase
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.api.CanceledStatus
import org.jetbrains.jps.builders.BuildResult
import org.jetbrains.jps.builders.CompileScopeTestBuilder
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.builders.impl.logging.ProjectBuilderLoggerBase
import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.JpsModuleRootModificationUtil
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.jps.incremental.JpsLookupStorageProvider
import org.jetbrains.kotlin.jps.incremental.KotlinDataContainerTarget
import org.jetbrains.kotlin.jps.incremental.getKotlinCache
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.keysToMap
import java.io.*
import java.util.*
import java.util.concurrent.Future
import kotlin.reflect.jvm.javaField

abstract class AbstractIncrementalJpsTest(
        private val allowNoFilesWithSuffixInTestData: Boolean = false,
        private val checkDumpsCaseInsensitively: Boolean = false,
        private val allowNoBuildLogFileInTestData: Boolean = false
) : BaseKotlinJpsBuildTestCase() {
    companion object {
        private val COMPILATION_FAILED = "COMPILATION FAILED"

        // change to "/tmp" or anything when default is too long (for easier debugging)
        private val TEMP_DIRECTORY_TO_USE = File(FileUtilRt.getTempDirectory())

        private val DEBUG_LOGGING_ENABLED = System.getProperty("debug.logging.enabled") == "true"
    }

    protected lateinit var testDataDir: File
    protected lateinit var workDir: File
    protected lateinit var projectDescriptor: ProjectDescriptor
    // is used to compare lookup dumps in a human readable way (lookup symbols are hashed in an actual lookup storage)
    protected lateinit var lookupsDuringTest: MutableSet<LookupSymbol>
    private var isICEnabledBackup: Boolean = false

    protected var mapWorkingToOriginalFile: MutableMap<File, File> = hashMapOf()

    protected open val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder()

    private fun enableDebugLogging() {
        com.intellij.openapi.diagnostic.Logger.setFactory(TestLoggerFactory::class.java)
        TestLoggerFactory.dumpLogToStdout("")
        TestLoggerFactory.enableDebugLogging(testRootDisposable, "#org")

        val console = ConsoleAppender()
        console.layout = PatternLayout("%d [%p|%c|%C{1}] %m%n")
        console.threshold = Level.ALL
        console.activateOptions()
        Logger.getRootLogger().addAppender(console)
    }

    private var systemPropertiesBackup = run {
        val props = System.getProperties()
        val output = ByteArrayOutputStream()
        props.store(output, "System properties backup")
        output.toByteArray()
    }

    private fun restoreSystemProperties() {
        val input = ByteArrayInputStream(systemPropertiesBackup)
        val props = Properties()
        props.load(input)
        System.setProperties(props)
    }

    override fun setUp() {
        super.setUp()
        lookupsDuringTest = hashSetOf()
        isICEnabledBackup = IncrementalCompilation.isEnabled()
        IncrementalCompilation.setIsEnabled(true)

        if (DEBUG_LOGGING_ENABLED) {
            enableDebugLogging()
        }
    }

    override fun tearDown() {
        restoreSystemProperties()
        (AbstractIncrementalJpsTest::myProject).javaField!![this] = null
        (AbstractIncrementalJpsTest::projectDescriptor).javaField!![this] = null
        (AbstractIncrementalJpsTest::systemPropertiesBackup).javaField!![this] = null
        lookupsDuringTest.clear()
        IncrementalCompilation.setIsEnabled(isICEnabledBackup)
        super.tearDown()
    }

    // JPS forces rebuild of all files when JVM constant has been changed and Callbacks.ConstantAffectionResolver
    // is not provided, so ConstantAffectionResolver is mocked with empty implementation
    protected open val mockConstantSearch: Callbacks.ConstantAffectionResolver?
        get() = MockConstantSearch(workDir)

    private fun build(scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().all()): MakeResult {
        val workDirPath = FileUtil.toSystemIndependentName(workDir.absolutePath)

        val logger = MyLogger(workDirPath)
        projectDescriptor = createProjectDescriptor(BuildLoggingManager(logger))

        val lookupTracker = TestLookupTracker()
        projectDescriptor.project.setTestingContext(TestingContext(lookupTracker, logger))

        try {
            val builder = IncProjectBuilder(projectDescriptor, BuilderRegistry.getInstance(), myBuildParams, CanceledStatus.NULL, mockConstantSearch, true)
            val buildResult = BuildResult()
            builder.addMessageHandler(buildResult)
            builder.build(scope.build(), false)

            lookupTracker.lookups.mapTo(lookupsDuringTest) { LookupSymbol(it.name, it.scopeFqName) }

            if (!buildResult.isSuccessful) {
                val errorMessages =
                        buildResult
                                .getMessages(BuildMessage.Kind.ERROR)
                                .map { it.messageText }
                                .map { it.replace("^.+:\\d+:\\s+".toRegex(), "").trim() }
                                .joinToString("\n")
                return MakeResult(logger.log + "$COMPILATION_FAILED\n" + errorMessages + "\n", true, null)
            }
            else {
                return MakeResult(logger.log, false, createMappingsDump(projectDescriptor))
            }
        }
        finally {
            projectDescriptor.dataManager.flush(false)
            projectDescriptor.release()
        }
    }

    private fun initialMake(): MakeResult {
        val makeResult = build()

        val initBuildLogFile = File(testDataDir, "init-build.log")
        if (initBuildLogFile.exists()) {
            UsefulTestCase.assertSameLinesWithFile(initBuildLogFile.absolutePath, makeResult.log)
        }
        else {
            assertFalse("Initial make failed:\n$makeResult", makeResult.makeFailed)
        }

        return makeResult
    }

    private fun make(): MakeResult {
        return build()
    }

    private fun rebuild(): MakeResult {
        return build(CompileScopeTestBuilder.rebuild().allModules())
    }

    private fun rebuildAndCheckOutput(makeOverallResult: MakeResult) {
        val outDir = File(getAbsolutePath("out"))
        val outAfterMake = File(getAbsolutePath("out-after-make"))

        if (outDir.exists()) {
            FileUtil.copyDir(outDir, outAfterMake)
        }

        val rebuildResult = rebuild()
        assertEquals("Rebuild failed: ${rebuildResult.makeFailed}, last make failed: ${makeOverallResult.makeFailed}. Rebuild result: $rebuildResult",
                     rebuildResult.makeFailed, makeOverallResult.makeFailed)

        if (!outAfterMake.exists()) {
            assertFalse(outDir.exists())
        }
        else {
            assertEqualDirectories(outDir, outAfterMake, makeOverallResult.makeFailed)
        }

        if (!makeOverallResult.makeFailed) {
            if (checkDumpsCaseInsensitively && rebuildResult.mappingsDump?.toLowerCase() == makeOverallResult.mappingsDump?.toLowerCase()) {
                // do nothing
            }
            else {
                TestCase.assertEquals(rebuildResult.mappingsDump, makeOverallResult.mappingsDump)
            }
        }

        FileUtil.delete(outAfterMake)
    }

    private fun clearCachesRebuildAndCheckOutput(makeOverallResult: MakeResult) {
        FileUtil.delete(BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot!!)

        rebuildAndCheckOutput(makeOverallResult)
    }

    private fun readModuleDependencies(): Map<String, List<DependencyDescriptor>>? {
        val dependenciesTxt = File(testDataDir, "dependencies.txt")
        if (!dependenciesTxt.exists()) return null

        val result = HashMap<String, List<DependencyDescriptor>>()
        for (line in dependenciesTxt.readLines()) {
            val split = line.split("->")
            val module = split[0]
            val dependencies = if (split.size > 1) split[1] else ""
            val dependencyList = dependencies.split(",").filterNot { it.isEmpty() }
            result[module] = dependencyList.map(::parseDependency)
        }

        return result
    }

    protected open fun createBuildLog(incrementalMakeResults: List<AbstractIncrementalJpsTest.MakeResult>): String =
            buildString {
                incrementalMakeResults.forEachIndexed { i, makeResult ->
                    if (i > 0) append("\n")
                    append("================ Step #${i + 1} =================\n\n")
                    append(makeResult.log)
                }
            }

    protected open fun doTest(testDataPath: String) {
        testDataDir = File(testDataPath)
        workDir = FileUtilRt.createTempDirectory(TEMP_DIRECTORY_TO_USE, "jps-build", null)
        Disposer.register(testRootDisposable, Disposable { FileUtilRt.delete(workDir) })

        val moduleNames = configureModules()
        initialMake()

        val otherMakeResults = performModificationsAndMake(moduleNames)
        val buildLogFile = buildLogFinder.findBuildLog(testDataDir)
        val logs = createBuildLog(otherMakeResults)

        if (buildLogFile != null && buildLogFile.exists()) {
            UsefulTestCase.assertSameLinesWithFile(buildLogFile.absolutePath, logs)
        }
        else if (!allowNoBuildLogFileInTestData) {
            throw IllegalStateException("No build log file in $testDataDir")
        }

        val lastMakeResult = otherMakeResults.last()
        rebuildAndCheckOutput(lastMakeResult)
        clearCachesRebuildAndCheckOutput(lastMakeResult)
    }

    private fun createMappingsDump(project: ProjectDescriptor) =
            createKotlinIncrementalCacheDump(project) + "\n\n\n" +
            createLookupCacheDump(project) + "\n\n\n" +
            createCommonMappingsDump(project) + "\n\n\n" +
            createJavaMappingsDump(project)

    private fun createKotlinIncrementalCacheDump(project: ProjectDescriptor): String {
        return buildString {
            for (target in project.allModuleTargets.sortedBy { it.presentableName }) {
                append("<target $target>\n")
                append(project.dataManager.getKotlinCache(target).dump())
                append("</target $target>\n\n\n")
            }
        }
    }

    private fun createLookupCacheDump(project: ProjectDescriptor): String {
        val sb = StringBuilder()
        val p = Printer(sb)
        p.println("Begin of Lookup Maps")
        p.println()

        val lookupStorage = project.dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider)
        lookupStorage.forceGC()
        p.print(lookupStorage.dump(lookupsDuringTest))

        p.println()
        p.println("End of Lookup Maps")
        return sb.toString()
    }

    private fun createCommonMappingsDump(project: ProjectDescriptor): String {
        val resultBuf = StringBuilder()
        val result = Printer(resultBuf)

        result.println("Begin of SourceToOutputMap")
        result.pushIndent()

        for (target in project.allModuleTargets) {
            result.println(target)
            result.pushIndent()

            val mapping = project.dataManager.getSourceToOutputMap(target)
            mapping.sources.sorted().forEach {
                val outputs = mapping.getOutputs(it)!!.sorted()
                if (outputs.isNotEmpty()) {
                    result.println("source $it -> $outputs")
                }
            }

            result.popIndent()
        }

        result.popIndent()
        result.println("End of SourceToOutputMap")

        return resultBuf.toString()
    }

    private fun createJavaMappingsDump(project: ProjectDescriptor): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        PrintStream(byteArrayOutputStream).use {
            project.dataManager.mappings.toStream(it)
        }
        return byteArrayOutputStream.toString()
    }

    protected data class MakeResult(val log: String, val makeFailed: Boolean, val mappingsDump: String?)

    private fun performModificationsAndMake(moduleNames: Set<String>?): List<MakeResult> {
        val results = arrayListOf<MakeResult>()
        val modifications = getModificationsToPerform(testDataDir, moduleNames, allowNoFilesWithSuffixInTestData, TouchPolicy.TIMESTAMP)

        for (step in modifications) {
            step.forEach { it.perform(workDir, mapWorkingToOriginalFile) }
            performAdditionalModifications(step)
            if (moduleNames == null) {
                preProcessSources(File(workDir, "src"))
            }
            else {
                moduleNames.forEach { preProcessSources(File(workDir, "$it/src")) }
            }

            results.add(make())
        }
        return results
    }

    protected open fun performAdditionalModifications(modifications: List<Modification>) {
    }

    // null means one module
    private fun configureModules(): Set<String>? {
        fun prepareModuleSources(moduleName: String?) {
            val sourceDirName = moduleName?.let { "$it/src" } ?: "src"
            val filePrefix = moduleName?.let { "${it}_" } ?: ""
            val sourceDestinationDir = File(workDir, sourceDirName)
            val sourcesMapping = copyTestSources(testDataDir, sourceDestinationDir, filePrefix)
            mapWorkingToOriginalFile.putAll(sourcesMapping)
            preProcessSources(sourceDestinationDir)
        }

        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject).outputUrl = JpsPathUtil.pathToUrl(getAbsolutePath("out"))

        val jdk = addJdk("my jdk")
        val moduleDependencies = readModuleDependencies()
        mapWorkingToOriginalFile = hashMapOf()

        val moduleNames: Set<String>?
        if (moduleDependencies == null) {
            addModule("module", arrayOf(getAbsolutePath("src")), null, null, jdk)
            prepareModuleSources(moduleName = null)
            moduleNames = null
        }
        else {
            val nameToModule = moduleDependencies.keys
                    .keysToMap { addModule(it, arrayOf(getAbsolutePath("$it/src")), null, null, jdk)!! }

            for ((moduleName, dependencies) in moduleDependencies) {
                val module = nameToModule[moduleName]!!

                for (dependency in dependencies) {
                    JpsModuleRootModificationUtil.addDependency(module, nameToModule[dependency.name],
                                                                JpsJavaDependencyScope.COMPILE, dependency.exported)
                }
            }

            for (module in nameToModule.values) {
                prepareModuleSources(module.name)
            }

            moduleNames = nameToModule.keys
        }
        AbstractKotlinJpsBuildTestCase.addKotlinStdlibDependency(myProject)
        AbstractKotlinJpsBuildTestCase.addKotlinTestDependency(myProject)
        return moduleNames
    }


    protected open fun preProcessSources(srcDir: File) {
    }

    override fun doGetProjectDir(): File? = workDir

    private class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase(), BuildLogger {

        private val dirtyFiles = ArrayList<File>()

        override fun actionsOnCacheVersionChanged(actions: List<CacheVersion.Action>) {
            if (actions.size > 1 && actions.any { it != CacheVersion.Action.DO_NOTHING }) {
                logLine("Actions after cache changed: $actions")
            }
        }

        override fun markedAsDirty(files: Iterable<File>) {
            dirtyFiles.addAll(files)
        }

        override fun buildStarted(context: CompileContext, chunk: ModuleChunk) {
            if (context.projectDescriptor.project.modules.size > 1) {
                logLine("Building ${chunk.modules.sortedBy { it.name }.joinToString { it.name }}")
            }
        }

        override fun buildFinished(exitCode: ModuleLevelBuilder.ExitCode) {

            if (dirtyFiles.isNotEmpty()) {
                logLine("Marked as dirty by Kotlin:")
                dirtyFiles
                        .map { FileUtil.toSystemIndependentName(it.path) }
                        .sorted()
                        .forEach { logLine(it) }
                dirtyFiles.clear()
            }
            logLine("Exit code: $exitCode")
            logLine("------------------------------------------")
        }

        private val logBuf = StringBuilder()
        val log: String
            get() = logBuf.toString()

        val compiledFiles = hashSetOf<File>()

        override fun isEnabled(): Boolean = true

        override fun logCompiledFiles(files: MutableCollection<File>?, builderName: String?, description: String?) {
            super.logCompiledFiles(files, builderName, description)

            if (builderName == KotlinBuilder.KOTLIN_BUILDER_NAME) {
                compiledFiles.addAll(files!!)
            }
        }

        override fun logLine(message: String?) {
            logBuf.append(KotlinTestUtils.replaceHashWithStar(message!!.replace("^$rootPath/".toRegex(), "  "))).append('\n')
        }
    }
}

private class MockConstantSearch(private val workDir: File) : Callbacks.ConstantAffectionResolver {
    override fun request(
        ownerClassName: String,
        fieldName: String,
        accessFlags: Int,
        fieldRemoved: Boolean,
        accessChanged: Boolean
    ): Future<Callbacks.ConstantAffection> {
        val affectedFiles = workDir.walk().filter { it.isFile && it.isNameUsage() }
        return FixedFuture(Callbacks.ConstantAffection(affectedFiles.toList()))
    }

    private fun File.isNameUsage(): Boolean =
            name.equals("usage.kt", ignoreCase = true)
            || name.equals("usage.java", ignoreCase = true)
}

internal val ProjectDescriptor.allModuleTargets: Collection<ModuleBuildTarget>
    get() = buildTargetIndex.allTargets.filterIsInstance<ModuleBuildTarget>()

private class DependencyDescriptor(val name: String, val exported: Boolean)

private fun parseDependency(dependency: String): DependencyDescriptor =
        DependencyDescriptor(dependency.removeSuffix(EXPORTED_SUFFIX), dependency.endsWith(EXPORTED_SUFFIX))

private val EXPORTED_SUFFIX = "[exported]"
