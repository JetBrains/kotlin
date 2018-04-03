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
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.jps.build.dependeciestxt.DependenciesTxt
import org.jetbrains.kotlin.jps.build.dependeciestxt.DependenciesTxtBuilder
import org.jetbrains.kotlin.jps.incremental.getKotlinCache
import org.jetbrains.kotlin.jps.incremental.withLookupStorage
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.jps.platforms.kotlinBuildTargets
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
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
    private var isJSICEnabledBackup: Boolean = false

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
        isJSICEnabledBackup = IncrementalCompilation.isEnabledForJs()

        IncrementalCompilation.setIsEnabled(true)
        IncrementalCompilation.setIsEnabledForJs(true)

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
        IncrementalCompilation.setIsEnabledForJs(isJSICEnabledBackup)
        super.tearDown()
    }

    // JPS forces rebuild of all files when JVM constant has been changed and Callbacks.ConstantAffectionResolver
    // is not provided, so ConstantAffectionResolver is mocked with empty implementation
    // Usages in Kotlin files are expected to be found by KotlinLookupConstantSearch
    private val mockConstantSearch: Callbacks.ConstantAffectionResolver?
        get() = MockJavaConstantSearch(workDir)

    private fun build(scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().allModules(), name: String? = null): MakeResult {
        val workDirPath = FileUtil.toSystemIndependentName(workDir.absolutePath)

        val logger = MyLogger(workDirPath)
        projectDescriptor = createProjectDescriptor(BuildLoggingManager(logger))

        val lookupTracker = TestLookupTracker()
        projectDescriptor.project.setTestingContext(TestingContext(lookupTracker, logger))

        try {
            val builder = IncProjectBuilder(
                projectDescriptor,
                BuilderRegistry.getInstance(),
                myBuildParams,
                CanceledStatus.NULL,
                mockConstantSearch,
                true
            )
            val buildResult = BuildResult()
            builder.addMessageHandler(buildResult)

            val finalScope = scope.build()
            builder.build(finalScope, false)

            lookupTracker.lookups.mapTo(lookupsDuringTest) { LookupSymbol(it.name, it.scopeFqName) }

            // for getting kotlin platform only
            val dummyCompileContext = CompileContextImpl.createContextForTests(finalScope, projectDescriptor)

            if (!buildResult.isSuccessful) {
                val errorMessages =
                    buildResult
                        .getMessages(BuildMessage.Kind.ERROR)
                        .map { it.messageText }
                        .map { it.replace("^.+:\\d+:\\s+".toRegex(), "").trim() }
                        .joinToString("\n")
                return MakeResult(
                    logger.log + "$COMPILATION_FAILED\n" + errorMessages + "\n",
                    true,
                    null,
                    name
                )
            } else {
                return MakeResult(
                    logger.log,
                    false,
                    createMappingsDump(projectDescriptor, dummyCompileContext),
                    name
                )
            }
        } finally {
            projectDescriptor.dataManager.flush(false)
            projectDescriptor.release()
        }
    }

    protected fun initialMake(): MakeResult {
        val makeResult = build(name = "initial")

        val initBuildLogFile = File(testDataDir, "init-build.log")
        if (initBuildLogFile.exists()) {
            UsefulTestCase.assertSameLinesWithFile(initBuildLogFile.absolutePath, makeResult.log)
        } else {
            assertFalse("Initial make failed:\n$makeResult", makeResult.makeFailed)
        }

        return makeResult
    }

    private fun make(name: String?): MakeResult {
        return build(name = name)
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
        assertEquals(
            "Rebuild failed: ${rebuildResult.makeFailed}, last make failed: ${makeOverallResult.makeFailed}. Rebuild result: $rebuildResult",
            rebuildResult.makeFailed, makeOverallResult.makeFailed
        )

        if (!outAfterMake.exists()) {
            assertFalse(outDir.exists())
        } else {
            assertEqualDirectories(outAfterMake, outDir, makeOverallResult.makeFailed)
        }

        if (!makeOverallResult.makeFailed) {
            if (checkDumpsCaseInsensitively && rebuildResult.mappingsDump?.toLowerCase() == makeOverallResult.mappingsDump?.toLowerCase()) {
                // do nothing
            } else {
                TestCase.assertEquals(rebuildResult.mappingsDump, makeOverallResult.mappingsDump)
            }
        }

        FileUtil.delete(outAfterMake)
    }

    private fun clearCachesRebuildAndCheckOutput(makeOverallResult: MakeResult) {
        FileUtil.delete(BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot!!)

        rebuildAndCheckOutput(makeOverallResult)
    }

    open protected val dependenciesTxtFile get() = File(testDataDir, "dependencies.txt")

    private fun readModuleDependencies(): DependenciesTxt? {
        val dependenciesTxtFile = dependenciesTxtFile
        if (!dependenciesTxtFile.exists()) return null

        return DependenciesTxtBuilder().readFile(dependenciesTxtFile)
    }

    protected open fun createBuildLog(incrementalMakeResults: List<AbstractIncrementalJpsTest.MakeResult>): String =
        buildString {
            incrementalMakeResults.forEachIndexed { i, makeResult ->
                if (i > 0) append("\n")
                if (makeResult.name != null) {
                    append("================ Step #${i + 1} ${makeResult.name} =================\n\n")
                } else {
                    append("================ Step #${i + 1} =================\n\n")
                }
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
        } else if (!allowNoBuildLogFileInTestData) {
            throw IllegalStateException("No build log file in $testDataDir")
        }

        val lastMakeResult = otherMakeResults.last()
        rebuildAndCheckOutput(lastMakeResult)
        clearCachesRebuildAndCheckOutput(lastMakeResult)
    }

    protected open fun doInitialMakeTest(testDataPath: String) {
        testDataDir = File(testDataPath)
        workDir = FileUtilRt.createTempDirectory(TEMP_DIRECTORY_TO_USE, "jps-build", null)
        Disposer.register(testRootDisposable, Disposable { FileUtilRt.delete(workDir) })

        configureModules()
        val results = initialMake()

        val buildLogFile = buildLogFinder.findBuildLog(testDataDir)

        if (buildLogFile != null && buildLogFile.exists()) {
            UsefulTestCase.assertSameLinesWithFile(buildLogFile.absolutePath, results.log)
        } else throw IllegalStateException("No build log file in $testDataDir")
    }

    private fun createMappingsDump(
        project: ProjectDescriptor,
        dummyCompileContext: CompileContext
    ) =
        createKotlinIncrementalCacheDump(project, dummyCompileContext) + "\n\n\n" +
                createLookupCacheDump(project) + "\n\n\n" +
                createCommonMappingsDump(project) + "\n\n\n" +
                createJavaMappingsDump(project)

    private fun createKotlinIncrementalCacheDump(
        project: ProjectDescriptor,
        dummyCompileContext: CompileContext
    ): String {
        return buildString {
            for (target in project.allModuleTargets.sortedBy { it.presentableName }) {
                append("<target $target>\n")
                append(project.dataManager.getKotlinCache(dummyCompileContext.kotlinBuildTargets[target]!!).dump())
                append("</target $target>\n\n\n")
            }
        }
    }

    private fun createLookupCacheDump(project: ProjectDescriptor): String {
        val sb = StringBuilder()
        val p = Printer(sb)
        p.println("Begin of Lookup Maps")
        p.println()

        project.dataManager.withLookupStorage { lookupStorage ->
            lookupStorage.forceGC()
            p.print(lookupStorage.dump(lookupsDuringTest))
        }

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

    protected data class MakeResult(
        val log: String,
        val makeFailed: Boolean,
        val mappingsDump: String?,
        val name: String?
    )

    open val testDataSrc: File
        get() = testDataDir

    private fun performModificationsAndMake(moduleNames: Set<String>?): List<MakeResult> {
        val results = arrayListOf<MakeResult>()
        val modifications = getModificationsToPerform(testDataSrc, moduleNames, allowNoFilesWithSuffixInTestData, TouchPolicy.TIMESTAMP)

        val stepsTxt = File(testDataSrc, "steps.txt")
        val modificationNames = if (stepsTxt.exists()) stepsTxt.readLines() else null

        modifications.forEachIndexed { index, step ->
            step.forEach { it.perform(workDir, mapWorkingToOriginalFile) }
            performAdditionalModifications(step)
            if (moduleNames == null) {
                preProcessSources(File(workDir, "src"))
            } else {
                moduleNames.forEach { preProcessSources(File(workDir, "$it/src")) }
            }

            val name = modificationNames?.getOrNull(index)
            val makeResult = make(name)
            results.add(makeResult)
        }
        return results
    }

    protected open fun performAdditionalModifications(modifications: List<Modification>) {
    }

    protected open fun generateModuleSources(dependenciesTxt: DependenciesTxt) {

    }

    protected open fun prepareModuleSources(module: DependenciesTxt.Module? = null) {
        if (module != null) {
            prepareModuleSourcesByName("${module.name}/src", "${module.name}_")
        } else {
            prepareModuleSourcesByName("src", "")
        }
    }

    protected fun prepareIndexedModuleSources(module: DependenciesTxt.Module) {
        prepareModuleSourcesByName("${module.name}/src", "${module.indexedName}_")
    }

    private fun prepareModuleSourcesByName(sourceDirName: String, filePrefix: String) {
        val sourceDestinationDir = File(workDir, sourceDirName)
        val sourcesMapping = copyTestSources(testDataSrc, sourceDestinationDir, filePrefix)
        mapWorkingToOriginalFile.putAll(sourcesMapping)
        preProcessSources(sourceDestinationDir)
    }

    // null means one module
    protected fun configureModules(): Set<String>? {
        val outputUrl = JpsPathUtil.pathToUrl(getAbsolutePath("out"))
        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject).outputUrl = outputUrl

        val jdk = addJdk("my jdk")
        val dependenciesTxt = readModuleDependencies()
        mapWorkingToOriginalFile = hashMapOf()

        val moduleNames: Set<String>?
        if (dependenciesTxt == null) {
            addModule("module", arrayOf(getAbsolutePath("src")), null, null, jdk)
            prepareModuleSources(module = null)
            moduleNames = null
        } else {
            dependenciesTxt.modules.forEach {
                val module = addModule(
                    it.name,
                    arrayOf(getAbsolutePath("${it.name}/src")),
                    null,
                    null,
                    jdk
                )!!

                val kotlinFacetSettings = it.kotlinFacetSettings
                if (kotlinFacetSettings != null) {
                    val compilerArguments = kotlinFacetSettings.compilerArguments
                    if (compilerArguments is K2MetadataCompilerArguments) {
                        val out = getAbsolutePath("${it.name}/out")
                        File(out).mkdirs()
                        compilerArguments.destination = out
                    }

                    module.container.setChild(
                        JpsKotlinFacetModuleExtension.KIND,
                        JpsKotlinFacetModuleExtension(kotlinFacetSettings)
                    )
                }

                it.jpsModule = module
            }

            dependenciesTxt.dependencies.forEach {
                JpsModuleRootModificationUtil.addDependency(
                    it.from.jpsModule,
                    it.to.jpsModule,
                    it.scope,
                    it.exported
                )
            }

            generateModuleSources(dependenciesTxt)
            dependenciesTxt.modules.forEach {
                prepareModuleSources(it)
            }

            moduleNames = dependenciesTxt.modules.map { it.name }.toSet()
        }
        AbstractKotlinJpsBuildTestCase.addKotlinStdlibDependency(myProject)
        AbstractKotlinJpsBuildTestCase.addKotlinTestDependency(myProject)
        return moduleNames
    }

    protected open fun preProcessSources(srcDir: File) {
    }

    override fun doGetProjectDir(): File? = workDir

    private class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase(), BuildLogger {
        private val markedDirtyBeforeRound = ArrayList<File>()
        private val markedDirtyAfterRound = ArrayList<File>()

        override fun actionsOnCacheVersionChanged(actions: List<CacheVersion.Action>) {
            if (actions.size > 1 && actions.any { it != CacheVersion.Action.DO_NOTHING }) {
                logLine("Actions after cache changed: $actions")
            }
        }

        override fun markedAsDirtyBeforeRound(files: Iterable<File>) {
            markedDirtyBeforeRound.addAll(files)
        }

        override fun markedAsDirtyAfterRound(files: Iterable<File>) {
            markedDirtyAfterRound.addAll(files)
        }

        override fun buildStarted(context: CompileContext, chunk: ModuleChunk) {
            if (!chunk.isDummy(context) && context.projectDescriptor.project.modules.size > 1) {
                logLine("Building ${chunk.modules.sortedBy { it.name }.joinToString { it.name }}")
            }
        }

        override fun afterBuildStarted(context: CompileContext, chunk: ModuleChunk) {
            logDirtyFiles(markedDirtyBeforeRound)
        }

        override fun buildFinished(exitCode: ModuleLevelBuilder.ExitCode) {
            logDirtyFiles(markedDirtyAfterRound)
            logLine("Exit code: $exitCode")
            logLine("------------------------------------------")
        }

        private fun logDirtyFiles(files: MutableList<File>) {
            if (files.isEmpty()) return

            logLine("Marked as dirty by Kotlin:")
            files.apply {
                map { FileUtil.toSystemIndependentName(it.path) }
                    .sorted()
                    .forEach { logLine(it) }

                clear()
            }
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

/**
 * Mocks Intellij Java constant search.
 * When JPS is run from Intellij, it sends find usages request to IDE (it only searches for references inside Java files).
 *
 * We rely on heuristics instead of precise usages search.
 * A Java file is considered affected if:
 * 1. It contains changed field name as a content substring.
 * 2. Its simple file name is not equal to a field's owner class simple name (to avoid recompiling field's declaration again)
 */
private class MockJavaConstantSearch(private val workDir: File) : Callbacks.ConstantAffectionResolver {
    override fun request(
        ownerClassName: String,
        fieldName: String,
        accessFlags: Int,
        fieldRemoved: Boolean,
        accessChanged: Boolean
    ): Future<Callbacks.ConstantAffection> {
        fun File.isAffected(): Boolean {
            if (!isJavaFile()) return false

            if (nameWithoutExtension == ownerClassName.substringAfterLast(".")) return false

            val code = readText()
            return code.contains(fieldName)
        }


        val affectedJavaFiles = workDir.walk().filter(File::isAffected).toList()
        return FixedFuture(Callbacks.ConstantAffection(affectedJavaFiles))
    }
}

internal val ProjectDescriptor.allModuleTargets: Collection<ModuleBuildTarget>
    get() = buildTargetIndex.allTargets.filterIsInstance<ModuleBuildTarget>()
