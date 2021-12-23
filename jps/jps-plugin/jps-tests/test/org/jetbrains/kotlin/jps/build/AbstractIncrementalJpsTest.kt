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
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.TestLoggerFactory
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ThrowableRunnable
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
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.JpsModuleRootModificationUtil
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.jps.build.dependeciestxt.ModulesTxt
import org.jetbrains.kotlin.jps.build.dependeciestxt.ModulesTxtBuilder
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.incremental.CacheAttributesDiff
import org.jetbrains.kotlin.jps.incremental.CacheVersionManager
import org.jetbrains.kotlin.jps.incremental.CompositeLookupsCacheAttributesManager
import org.jetbrains.kotlin.jps.incremental.getKotlinCache
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.jps.model.kotlinCommonCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinFacet
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.platform.orDefault
import org.jetbrains.kotlin.utils.Printer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.reflect.jvm.javaField

abstract class AbstractIncrementalJpsTest(
    private val allowNoFilesWithSuffixInTestData: Boolean = false,
    private val checkDumpsCaseInsensitively: Boolean = false
) : BaseKotlinJpsBuildTestCase() {
    companion object {
        private val COMPILATION_FAILED = "COMPILATION FAILED"

        // change to "/tmp" or anything when default is too long (for easier debugging)
        private val TEMP_DIRECTORY_TO_USE = File(FileUtilRt.getTempDirectory())

        private val DEBUG_LOGGING_ENABLED = System.getProperty("debug.logging.enabled") == "true"

        private const val ARGUMENTS_FILE_NAME = "args.txt"

        private fun parseAdditionalArgs(testDir: File): List<String> {
            return File(testDir, ARGUMENTS_FILE_NAME)
                .takeIf { it.exists() }
                ?.readText()
                ?.split(" ", "\n")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }

    protected lateinit var testDataDir: File
    protected lateinit var workDir: File
    protected lateinit var projectDescriptor: ProjectDescriptor
    protected lateinit var additionalCommandLineArguments: List<String>
    // is used to compare lookup dumps in a human readable way (lookup symbols are hashed in an actual lookup storage)
    protected lateinit var lookupsDuringTest: MutableSet<LookupSymbol>
    private var isJvmICEnabledBackup: Boolean = false
    private var isJsICEnabledBackup: Boolean = false

    protected var mapWorkingToOriginalFile: MutableMap<File, File> = hashMapOf()

    lateinit var kotlinCompileContext: KotlinCompileContext

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

    private val enableICFixture = EnableICFixture()

    override fun setUp() {
        super.setUp()

        enableICFixture.setUp()
        lookupsDuringTest = hashSetOf()

        if (DEBUG_LOGGING_ENABLED) {
            enableDebugLogging()
        }
    }

    override fun tearDown() {
        try {
            restoreSystemProperties()

            (AbstractIncrementalJpsTest::myProject).javaField!![this] = null
            (AbstractIncrementalJpsTest::projectDescriptor).javaField!![this] = null
            (AbstractIncrementalJpsTest::systemPropertiesBackup).javaField!![this] = null
        } finally {
            RunAll(
                ThrowableRunnable { lookupsDuringTest.clear() },
                ThrowableRunnable { enableICFixture.tearDown() },
                ThrowableRunnable { super.tearDown() }
            ).run()
        }
    }

    // JPS forces rebuild of all files when JVM constant has been changed and Callbacks.ConstantAffectionResolver
    // is not provided, so ConstantAffectionResolver is mocked with empty implementation
    // Usages in Kotlin files are expected to be found by KotlinLookupConstantSearch
    protected open val mockConstantSearch: Callbacks.ConstantAffectionResolver?
        get() = MockJavaConstantSearch(workDir)

    private fun build(
        name: String?,
        scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().allModules()
    ): MakeResult {
        val workDirPath = FileUtil.toSystemIndependentName(workDir.absolutePath)

        val logger = MyLogger(workDirPath)
        projectDescriptor = createProjectDescriptor(BuildLoggingManager(logger))

        val lookupTracker = TestLookupTracker()
        val testingContext = TestingContext(lookupTracker, logger)
        projectDescriptor.project.setTestingContext(testingContext)

        try {
            val builder = IncProjectBuilder(
                projectDescriptor,
                BuilderRegistry.getInstance(),
                myBuildParams,
                CanceledStatus.NULL,
                true
            )
            val buildResult = BuildResult()
            builder.addMessageHandler(buildResult)
            val finalScope = scope.build()
            projectDescriptor.project.kotlinCommonCompilerArguments = projectDescriptor.project.kotlinCommonCompilerArguments.apply {
                updateCommandLineArguments(this)
            }

            builder.build(finalScope, false)

            // testingContext.kotlinCompileContext is initialized in KotlinBuilder.initializeKotlinContext
            kotlinCompileContext = testingContext.kotlinCompileContext!!

            lookupTracker.lookups.mapTo(lookupsDuringTest) { LookupSymbol(it.name, it.scopeFqName) }

            if (!buildResult.isSuccessful) {
                val errorMessages =
                    buildResult
                        .getMessages(BuildMessage.Kind.ERROR)
                        .map { it.messageText }
                        .map { it.replace("^.+:\\d+:\\s+".toRegex(), "").trim() }
                        .joinToString("\n")
                return MakeResult(
                    log = logger.log + "$COMPILATION_FAILED\n" + errorMessages + "\n",
                    makeFailed = true,
                    mappingsDump = null,
                    name = name
                )
            } else {
                return MakeResult(
                    log = logger.log,
                    makeFailed = false,
                    mappingsDump = createMappingsDump(projectDescriptor, kotlinCompileContext, lookupsDuringTest),
                    name = name
                )
            }
        } finally {
            projectDescriptor.dataManager.flush(false)
            projectDescriptor.release()
        }
    }

    private fun initialMake(): MakeResult {
        val makeResult = build(null)

        val initBuildLogFile = File(testDataDir, "init-build.log")
        if (initBuildLogFile.exists()) {
            UsefulTestCase.assertSameLinesWithFile(initBuildLogFile.absolutePath, makeResult.log)
        } else {
            assertFalse("Initial make failed:\n$makeResult", makeResult.makeFailed)
        }

        return makeResult
    }

    private fun make(name: String?): MakeResult {
        return build(name)
    }

    private fun rebuild(): MakeResult {
        return build(null, CompileScopeTestBuilder.rebuild().allModules())
    }

    private fun updateCommandLineArguments(arguments: CommonCompilerArguments) {
        parseCommandLineArguments(additionalCommandLineArguments, arguments)
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
            assertEqualDirectories(outDir, outAfterMake, makeOverallResult.makeFailed)
        }

        if (!makeOverallResult.makeFailed) {
            if (checkDumpsCaseInsensitively && rebuildResult.mappingsDump.equals(makeOverallResult.mappingsDump, ignoreCase = true)) {
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

    open val modulesTxtFile
        get() = File(testDataDir, "dependencies.txt")

    private fun readModulesTxt(): ModulesTxt? {
        var actualModulesTxtFile = modulesTxtFile

        if (!actualModulesTxtFile.exists()) {
            // also try `"_${fileName}.txt"`. Useful for sorting files in IDE.
            actualModulesTxtFile = modulesTxtFile.parentFile.resolve("_" + modulesTxtFile.name)
            if (!actualModulesTxtFile.exists()) return null
        }

        return ModulesTxtBuilder().readFile(actualModulesTxtFile)
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
        workDir = FileUtilRt.createTempDirectory(TEMP_DIRECTORY_TO_USE, "aijt-jps-build", null)
        additionalCommandLineArguments = parseAdditionalArgs(File(testDataPath))
        val buildLogFile = buildLogFinder.findBuildLog(testDataDir)
        Disposer.register(testRootDisposable, Disposable { FileUtilRt.delete(workDir) })

        val modulesTxt = configureModules()
        if (modulesTxt?.muted == true) return

        initialMake()

        val otherMakeResults = performModificationsAndMake(
            modulesTxt?.modules?.map { it.name },
            hasBuildLog = buildLogFile != null
        )

        buildLogFile?.let {
            val logs = createBuildLog(otherMakeResults)
            UsefulTestCase.assertSameLinesWithFile(buildLogFile.absolutePath, logs)

            val lastMakeResult = otherMakeResults.last()
            clearCachesRebuildAndCheckOutput(lastMakeResult)
        }
    }

    protected data class MakeResult(
        val log: String,
        val makeFailed: Boolean,
        val mappingsDump: String?,
        val name: String? = null
    )

    open val testDataSrc: File
        get() = testDataDir

    private fun performModificationsAndMake(
        moduleNames: Collection<String>?,
        hasBuildLog: Boolean
    ): List<MakeResult> {
        val results = arrayListOf<MakeResult>()
        val modifications = getModificationsToPerform(
            testDataSrc,
            moduleNames,
            allowNoFilesWithSuffixInTestData = allowNoFilesWithSuffixInTestData || !hasBuildLog,
            touchPolicy = TouchPolicy.TIMESTAMP
        )

        if (!hasBuildLog) {
            check(modifications.size == 1 && modifications.single().isEmpty()) {
                "Bad test data: build steps are provided, but there is no `build.log` file"
            }
            return results
        }

        val stepsTxt = File(testDataSrc, "_steps.txt")
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

    protected open fun generateModuleSources(modulesTxt: ModulesTxt) = Unit

    // null means one module
    private fun configureModules(): ModulesTxt? {
        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject).outputUrl =
                JpsPathUtil.pathToUrl(getAbsolutePath("out"))

        val jdk = addJdk("my jdk")
        val modulesTxt = readModulesTxt()
        mapWorkingToOriginalFile = hashMapOf()

        if (modulesTxt == null) configureSingleModuleProject(jdk)
        else configureMultiModuleProject(modulesTxt, jdk)

        overrideModuleSettings()
        configureRequiredLibraries()

        return modulesTxt
    }

    open fun overrideModuleSettings() {
    }

    private fun configureSingleModuleProject(jdk: JpsSdk<JpsDummyElement>?) {
        addModule("module", arrayOf(getAbsolutePath("src")), null, null, jdk)

        val sourceDestinationDir = File(workDir, "src")
        val sourcesMapping = copyTestSources(testDataDir, File(workDir, "src"), "")
        mapWorkingToOriginalFile.putAll(sourcesMapping)

        preProcessSources(sourceDestinationDir)
    }

    protected open val ModulesTxt.Module.sourceFilePrefix: String
        get() = "${name}_"

    private fun configureMultiModuleProject(
        modulesTxt: ModulesTxt,
        jdk: JpsSdk<JpsDummyElement>?
    ) {
        modulesTxt.modules.forEach { module ->
            module.jpsModule = addModule(
                module.name,
                arrayOf(getAbsolutePath("${module.name}/src")),
                null,
                null,
                jdk
            )!!

            val kotlinFacetSettings = module.kotlinFacetSettings
            if (kotlinFacetSettings != null) {
                val compilerArguments = kotlinFacetSettings.compilerArguments
                if (compilerArguments is K2MetadataCompilerArguments) {
                    val out = getAbsolutePath("${module.name}/out")
                    File(out).mkdirs()
                    compilerArguments.destination = out
                } else if (compilerArguments is K2JVMCompilerArguments) {
                    compilerArguments.disableDefaultScriptingPlugin = true
                }

                module.jpsModule.container.setChild(
                    JpsKotlinFacetModuleExtension.KIND,
                    JpsKotlinFacetModuleExtension(kotlinFacetSettings)
                )
            }
        }

        modulesTxt.dependencies.forEach {
            JpsModuleRootModificationUtil.addDependency(
                it.from.jpsModule, it.to.jpsModule,
                it.scope, it.exported
            )
        }

        // configure module contents
        generateModuleSources(modulesTxt)
        modulesTxt.modules.forEach { module ->
            val sourceDirName = "${module.name}/src"
            val sourceDestinationDir = File(workDir, sourceDirName)
            val sourcesMapping = copyTestSources(testDataSrc, sourceDestinationDir, module.sourceFilePrefix)
            mapWorkingToOriginalFile.putAll(sourcesMapping)

            preProcessSources(sourceDestinationDir)
        }
    }

    private fun configureRequiredLibraries() {
        myProject.modules.forEach { module ->
            val platformKind = module.kotlinFacet?.settings?.targetPlatform?.idePlatformKind.orDefault()

            when {
                platformKind.isJvm -> {
                    JpsModuleRootModificationUtil.addDependency(module, requireLibrary(KotlinJpsLibrary.JvmStdLib))
                    JpsModuleRootModificationUtil.addDependency(module, requireLibrary(KotlinJpsLibrary.JvmTest))
                }
                platformKind.isJavaScript -> {
                    JpsModuleRootModificationUtil.addDependency(module, requireLibrary(KotlinJpsLibrary.JsStdLib))
                    JpsModuleRootModificationUtil.addDependency(module, requireLibrary(KotlinJpsLibrary.JsTest))
                }
            }
        }
    }

    protected open fun preProcessSources(srcDir: File) {
    }

    override fun doGetProjectDir(): File? = workDir

    internal class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase(), TestingBuildLogger {
        private val markedDirtyBeforeRound = ArrayList<File>()
        private val markedDirtyAfterRound = ArrayList<File>()
        private val customMessages = mutableListOf<String>()

        override fun invalidOrUnusedCache(
            chunk: KotlinChunk?,
            target: KotlinModuleBuildTarget<*>?,
            attributesDiff: CacheAttributesDiff<*>
        ) {
            val cacheManager = attributesDiff.manager
            val cacheTitle = when (cacheManager) {
                is CacheVersionManager -> "Local cache for ${chunk ?: target}"
                is CompositeLookupsCacheAttributesManager -> "Lookups cache"
                else -> error("Unknown cache manager $cacheManager")
            }

            logLine("$cacheTitle are ${attributesDiff.status}")
        }

        override fun markedAsDirtyBeforeRound(files: Iterable<File>) {
            markedDirtyBeforeRound.addAll(files)
        }

        override fun markedAsDirtyAfterRound(files: Iterable<File>) {
            markedDirtyAfterRound.addAll(files)
        }

        override fun chunkBuildStarted(context: CompileContext, chunk: ModuleChunk) {
            logDirtyFiles(markedDirtyBeforeRound) // files can be marked as dirty during build start (KotlinCompileContext initialization)

            if (!chunk.isDummy(context) && context.projectDescriptor.project.modules.size > 1) {
                logLine("Building ${chunk.modules.sortedBy { it.name }.joinToString { it.name }}")
            }
        }

        override fun afterChunkBuildStarted(context: CompileContext, chunk: ModuleChunk) {
            logDirtyFiles(markedDirtyBeforeRound)
        }

        override fun addCustomMessage(message: String) {
            customMessages.add(message)
        }

        override fun buildFinished(exitCode: ModuleLevelBuilder.ExitCode) {
            customMessages.forEach {
                logLine(it)
            }
            customMessages.clear()
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
            logBuf.append(message!!.replace("^$rootPath/".toRegex(), "  ")).append('\n')
        }
    }
}

private fun createMappingsDump(
    project: ProjectDescriptor,
    kotlinContext: KotlinCompileContext,
    lookupsDuringTest: Set<LookupSymbol>
) = createKotlinCachesDump(project, kotlinContext, lookupsDuringTest) + "\n\n\n" +
        createCommonMappingsDump(project) + "\n\n\n" +
        createJavaMappingsDump(project)

internal fun createKotlinCachesDump(
    project: ProjectDescriptor,
    kotlinContext: KotlinCompileContext,
    lookupsDuringTest: Set<LookupSymbol>
) = createKotlinIncrementalCacheDump(project, kotlinContext) + "\n\n\n" +
        createLookupCacheDump(kotlinContext, lookupsDuringTest)

private fun createKotlinIncrementalCacheDump(
    project: ProjectDescriptor,
    kotlinContext: KotlinCompileContext
): String {
    return buildString {
        for (target in project.allModuleTargets.sortedBy { it.presentableName }) {
            val kotlinCache = project.dataManager.getKotlinCache(kotlinContext.targetsBinding[target])
            if (kotlinCache != null) {
                append("<target $target>\n")
                append(kotlinCache.dump())
                append("</target $target>\n\n\n")
            }
        }
    }
}

private fun createLookupCacheDump(kotlinContext: KotlinCompileContext, lookupsDuringTest: Set<LookupSymbol>): String {
    val sb = StringBuilder()
    val p = Printer(sb)
    p.println("Begin of Lookup Maps")
    p.println()

    kotlinContext.lookupStorageManager.withLookupStorage { lookupStorage ->
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

internal val ProjectDescriptor.allModuleTargets: Collection<ModuleBuildTarget>
    get() = buildTargetIndex.allTargets.filterIsInstance<ModuleBuildTarget>()

private val EXPORTED_SUFFIX = "[exported]"
