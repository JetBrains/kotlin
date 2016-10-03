package org.jetbrains.kotlin.gradle.tasks

import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.GradleException
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.annotation.SourceAnnotationsRegistry
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.SourceRetentionAnnotationHandler
import org.jetbrains.kotlin.incremental.snapshots.FileCollectionDiff
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File
import java.util.*
import kotlin.properties.Delegates

const val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"
const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val CACHES_DIR_NAME = "caches"
const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
const val USING_EXPERIMENTAL_INCREMENTAL_MESSAGE = "Using experimental kotlin incremental compilation"

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractCompile() {
    abstract protected val compiler: CLICompiler<T>
    abstract protected fun populateCompilerArguments(): T

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    var incremental: Boolean = false
        get() = field
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
            System.setProperty("kotlin.incremental.compilation", value.toString())
            System.setProperty("kotlin.incremental.compilation.experimental", value.toString())
        }

    internal var compilerCalled: Boolean = false
    // TODO: consider more reliable approach (see usage)
    internal var anyClassesCompiled: Boolean = false
    internal var friendTaskName: String? = null
    internal var javaOutputDir: File? = null
    internal var sourceSetName: String by Delegates.notNull()
    protected val moduleName: String
            get() = "${project.name}_$sourceSetName"

    override fun compile() {
        assert(false, { "unexpected call to compile()" })
    }

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs): Unit {
        logger.kotlinDebug("all sources ${getSource().joinToString { it.path }}")
        logger.kotlinDebug("is incremental == ${inputs.isIncremental}")
        val modified = arrayListOf<File>()
        val removed = arrayListOf<File>()
        if (inputs.isIncremental) {
            inputs.outOfDate { modified.add(it.file) }
            inputs.removed { removed.add(it.file) }
        }
        logger.kotlinDebug("modified ${modified.joinToString { it.path }}")
        logger.kotlinDebug("removed ${removed.joinToString { it.path }}")
        val sources = getKotlinSources()
        if (sources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        val args = populateCompilerArguments()
        compilerCalled = true
        callCompiler(args, sources, inputs.isIncremental, modified, removed)
    }

    private fun getKotlinSources(): List<File> = (getSource() as Iterable<File>).filter { it.isKotlinFile() }

    protected fun File.isKotlinFile(): Boolean =
            FilenameUtils.isExtension(name.toLowerCase(), listOf("kt", "kts"))

    protected abstract fun callCompiler(args: T, sources: List<File>, isIncremental: Boolean, modified: List<File>, removed: List<File>)

}

open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
    override val compiler = K2JVMCompiler()

    internal var parentKotlinOptionsImpl: KotlinJvmOptionsImpl? = null
    private val kotlinOptionsImpl = KotlinJvmOptionsImpl()
    override val kotlinOptions: KotlinJvmOptions
            get() = kotlinOptionsImpl

    private val sourceRoots = HashSet<File>()

    internal val taskBuildDirectory: File
        get() = File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name).apply { mkdirs() }
    private val cacheDirectory: File by lazy { File(taskBuildDirectory, CACHES_DIR_NAME) }
    private val dirtySourcesSinceLastTimeFile: File by lazy { File(taskBuildDirectory, DIRTY_SOURCES_FILE_NAME) }
    private val lastBuildInfoFile: File by lazy { File(taskBuildDirectory, LAST_BUILD_INFO_FILE_NAME) }
    private val cacheVersions by lazy {
        listOf(normalCacheVersion(taskBuildDirectory),
               experimentalCacheVersion(taskBuildDirectory),
               dataContainerCacheVersion(taskBuildDirectory),
               gradleCacheVersion(taskBuildDirectory))
    }
    internal val isCacheFormatUpToDate: Boolean
        get() {
            if (!incremental) return true

            return cacheVersions.all { it.checkVersion() == CacheVersion.Action.DO_NOTHING }
        }

    private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null
    private val additionalClasspath = arrayListOf<File>()
    private val compileClasspath: Iterable<File>
            get() = (classpath + additionalClasspath)
                    .filterTo(LinkedHashSet(), File::exists)
    private val kapt2GeneratedSourcesDir: File
            get() = File(project.buildDir, "generated/source/kapt2")


    internal val kaptOptions = KaptOptions()
    internal val pluginOptions = CompilerPluginOptions()
    internal var artifactDifferenceRegistry: ArtifactDifferenceRegistry? = null
    internal var artifactFile: File? = null
    // created only if kapt2 is active
    internal var sourceAnnotationsRegistry: SourceAnnotationsRegistry? = null

    override fun populateCompilerArguments(): K2JVMCompilerArguments {
        val args = K2JVMCompilerArguments().apply { fillDefaultValues() }

        handleKaptProperties()
        args.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        args.pluginOptions = pluginOptions.arguments.toTypedArray()
        args.moduleName = moduleName

        friendTaskName?.let addFriendPathForTestTask@ { friendKotlinTaskName ->
            val friendTask = project.getTasksByName(friendKotlinTaskName, /* recursive = */false).firstOrNull() as? KotlinCompile ?: return@addFriendPathForTestTask
            args.friendPaths = arrayOf(friendTask.javaOutputDir!!.absolutePath)
            args.moduleName = friendTask.moduleName
            logger.kotlinDebug("java destination directory for production = ${friendTask.javaOutputDir}")
        }

        parentKotlinOptionsImpl?.updateArguments(args)
        kotlinOptionsImpl.updateArguments(args)

        fun dumpPaths(files: Iterable<File>): String =
            "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"

        logger.kotlinDebug { "$name destinationDir = $destinationDir" }
        logger.kotlinDebug { "$name source roots: ${dumpPaths(sourceRoots)}" }
        logger.kotlinDebug { "$name java source roots: ${dumpPaths(getJavaSourceRoots())}" }
        return args
    }

    override fun callCompiler(args: K2JVMCompilerArguments, sources: List<File>, isIncrementalRequested: Boolean, modified: List<File>, removed: List<File>) {
        fun relativePathOrCanonical(file: File): String =
                file.relativeToOrNull(project.rootProject.projectDir)?.path
                        ?: file.canonicalPath

        fun filesToString(files: Iterable<File>) =
                "[" + files.map(::relativePathOrCanonical).sorted().joinToString(separator = ", \n") + "]"

        val outputDir = destinationDir
        var currentRemoved = removed.filter { it.isKotlinFile() }
        val allGeneratedFiles = hashSetOf<GeneratedFile<TargetId>>()
        val logAction = { logStr: String -> logger.kotlinInfo(logStr) }

        fun getClasspathChanges(modifiedClasspath: List<File>, lastBuildInfo: BuildInfo?): ChangesEither {
            if (modifiedClasspath.isEmpty()) {
                logger.kotlinDebug { "No classpath changes" }
                return ChangesEither.Known()
            }
            if (artifactDifferenceRegistry == null) {
                logger.kotlinDebug { "No artifact history provider" }
                return ChangesEither.Unknown()
            }

            val lastBuildTS = lastBuildInfo?.startTS
            if (lastBuildTS == null) {
                logger.kotlinDebug { "Could not determine last build timestamp" }
                return ChangesEither.Unknown()
            }

            val symbols = HashSet<LookupSymbol>()
            val fqNames = HashSet<FqName>()
            for (file in modifiedClasspath) {
                val diffs = artifactDifferenceRegistry!![file]
                if (diffs == null) {
                    logger.kotlinDebug { "Could not get changes for file: $file" }
                    return ChangesEither.Unknown()
                }

                val (beforeLastBuild, afterLastBuild) = diffs.partition { it.buildTS < lastBuildTS }
                if (beforeLastBuild.isEmpty()) {
                    logger.kotlinDebug { "No known build preceding timestamp $lastBuildTS for file $file" }
                    return ChangesEither.Unknown()
                }

                afterLastBuild.forEach {
                    symbols.addAll(it.dirtyData.dirtyLookupSymbols)
                    fqNames.addAll(it.dirtyData.dirtyClassesFqNames)
                }
            }

            return ChangesEither.Known(symbols, fqNames)
        }

        fun calculateSourcesToCompile(javaFilesProcessor: ChangedJavaFilesProcessor, caches: IncrementalCachesManager, lastBuildInfo: BuildInfo?): Pair<Set<File>, Boolean> {
            fun rebuild(reason: String): Pair<Set<File>, Boolean> {
                logger.kotlinInfo("Non-incremental compilation will be performed: $reason")
                caches.clean()
                dirtySourcesSinceLastTimeFile.delete()
                return Pair(sources.toSet(), false)
            }

            if (!incremental) return rebuild("incremental compilation is not enabled")
            if (!isIncrementalRequested) return rebuild("inputs' changes are unknown (first or clean build)")

            val removedClassFiles = removed.filter(File::hasClassFileExtension)
            if (removedClassFiles.any()) return rebuild("Removed class files: ${filesToString(removedClassFiles)}")

            val modifiedClassFiles = modified.filter(File::hasClassFileExtension)
            if (modifiedClassFiles.any()) return rebuild("Modified class files: ${filesToString(modifiedClassFiles)}")

            val modifiedClasspathEntries = modified.filter { it in classpath }
            val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo)
            if (classpathChanges is ChangesEither.Unknown) {
                return rebuild("could not get changes from modified classpath entries: ${filesToString(modifiedClasspathEntries)}")
            }
            if (classpathChanges !is ChangesEither.Known) {
                throw AssertionError("Unknown implementation of ChangesEither: ${classpathChanges.javaClass}")
            }
            val javaFilesDiff = FileCollectionDiff(
                    newOrModified = modified.filter { it.isJavaFile() },
                    removed = removed.filter { it.isJavaFile() })
            val javaFilesChanges = javaFilesProcessor.process(javaFilesDiff)
            val affectedJavaSymbols = when (javaFilesChanges) {
                is ChangesEither.Known -> javaFilesChanges.lookupSymbols
                is ChangesEither.Unknown -> return rebuild("Could not get changes for java files")
            }

            val dirtyFiles = modified.filter { it.isKotlinFile() }.toMutableSet()
            val lookupSymbols = HashSet<LookupSymbol>()
            lookupSymbols.addAll(affectedJavaSymbols)
            lookupSymbols.addAll(classpathChanges.lookupSymbols)

            if (lookupSymbols.any()) {
                val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, logAction, ::relativePathOrCanonical)
                dirtyFiles.addAll(dirtyFilesFromLookups)
            }

            val dirtyClassesFqNames = classpathChanges.fqNames.flatMap { withSubtypes(it, listOf(caches.incrementalCache)) }
            if (dirtyClassesFqNames.any()) {
                val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassesFqNames, logAction, ::relativePathOrCanonical)
                dirtyFiles.addAll(dirtyFilesFromFqNames)
            }

            if (dirtySourcesSinceLastTimeFile.exists()) {
                val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter(File::exists)
                if (files.isNotEmpty()) {
                    logger.kotlinDebug { "Source files added since last compilation: $files" }
                }

                dirtyFiles.addAll(files)
            }

            return Pair(dirtyFiles, true)
        }

        fun cleanupOnError() {
            logger.kotlinInfo("deleting $destinationDir on error")
            destinationDir.deleteRecursively()
        }

        fun processCompilerExitCode(exitCode: ExitCode) {
            if (exitCode != ExitCode.OK) {
                cleanupOnError()
            }

            when (exitCode) {
                ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
                ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
                ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
                ExitCode.OK -> {
                    sourceAnnotationsRegistry?.flush()
                    cacheVersions.forEach { it.saveIfNeeded() }
                    logger.kotlinInfo("Compilation succeeded")
                }
            }
        }

        if (!incremental) {
            anyClassesCompiled = true
            processCompilerExitCode(compileNotIncremental(sources, outputDir, args))
            return
        }

        logger.warn(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
        anyClassesCompiled = false
        val javaFilesProcessor = ChangedJavaFilesProcessor()
        val targetId = TargetId(name = args.moduleName, type = "java-production")
        val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
        logger.kotlinDebug { "Last Kotlin Build info for task $path -- $lastBuildInfo" }
        val caches = IncrementalCachesManager(targetId, cacheDirectory, outputDir)

        // TODO: decide what to do if no files are considered dirty - rebuild or skip the module
        var (sourcesToCompile, isIncrementalDecided) = calculateSourcesToCompile(javaFilesProcessor, caches, lastBuildInfo)

        if (isIncrementalDecided) {
            additionalClasspath.add(destinationDir)
        }
        else {
            // there is no point in updating annotation file since all files will be compiled anyway
            kaptAnnotationsFileUpdater = null
        }

        logger.kotlinDebug { "Artifact to register difference for task $path: $artifactFile" }
        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis())
        BuildInfo.write(currentBuildInfo, lastBuildInfoFile)
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()

        var exitCode = ExitCode.OK
        while (sourcesToCompile.any() || currentRemoved.any()) {
            val removedAndModified = (sourcesToCompile + currentRemoved).toList()
            val outdatedClasses = caches.incrementalCache.classesBySources(removedAndModified)
            caches.incrementalCache.markOutputClassesDirty(removedAndModified)
            caches.incrementalCache.removeClassfilesBySources(removedAndModified)

            // can be empty if only removed sources are present
            if (sourcesToCompile.isNotEmpty()) {
                logger.kotlinInfo("compile iteration: ${sourcesToCompile.joinToString(transform = ::relativePathOrCanonical)}")
            }

            val (existingSource, nonExistingSource) = sourcesToCompile.partition { it.isFile }
            assert(nonExistingSource.isEmpty()) { "Trying to compile removed files: ${nonExistingSource.map(::relativePathOrCanonical)}" }

            val text = existingSource.map { it.canonicalPath }.joinToString(separator = System.getProperty("line.separator"))
            dirtySourcesSinceLastTimeFile.writeText(text)

            val compilerOutput = compileChanged(listOf(targetId), existingSource.toSet(), outputDir, args, { caches.incrementalCache }, lookupTracker)
            exitCode = compilerOutput.exitCode

            if (exitCode == ExitCode.OK) {
                dirtySourcesSinceLastTimeFile.delete()
                kaptAnnotationsFileUpdater?.updateAnnotations(outdatedClasses)
            }
            else {
                kaptAnnotationsFileUpdater?.revert()
                break
            }

            allGeneratedFiles.addAll(compilerOutput.generatedFiles)
            val compilationResult = updateIncrementalCaches(listOf(targetId), compilerOutput.generatedFiles,
                                                            compiledWithErrors = exitCode != ExitCode.OK,
                                                            getIncrementalCache = { caches.incrementalCache })

            caches.lookupCache.update(lookupTracker, sourcesToCompile, currentRemoved)

            val generatedJavaFiles = kapt2GeneratedSourcesDir.walk().filter { it.isJavaFile() }.toList()
            val generatedJavaFilesDiff = caches.incrementalCache.compareAndUpdateFileSnapshots(generatedJavaFiles)

            if (!isIncrementalDecided) {
                artifactFile?.let { artifactDifferenceRegistry?.remove(it) }
                break
            }

            val (dirtyLookupSymbols, dirtyClassFqNames) = compilationResult.getDirtyData(listOf(caches.incrementalCache), logAction)
            val generatedJavaFilesChanges = javaFilesProcessor.process(generatedJavaFilesDiff)
            val dirtyKotlinFilesFromJava = when (generatedJavaFilesChanges) {
                is ChangesEither.Unknown -> {
                    logger.kotlinDebug { "Could not get changes for generated java files, recompiling all kotlin" }
                    isIncrementalDecided = false
                    sources.toSet()
                }
                is ChangesEither.Known -> {
                    mapLookupSymbolsToFiles(caches.lookupCache, generatedJavaFilesChanges.lookupSymbols, logAction, ::relativePathOrCanonical, excludes = sourcesToCompile)
                }
                else -> throw IllegalStateException("Unknown ChangesEither implementation: $generatedJavaFiles")
            }
            sourcesToCompile = dirtyKotlinFilesFromJava +
                               mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, logAction, ::relativePathOrCanonical, excludes = sourcesToCompile) +
                               mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassFqNames, logAction, ::relativePathOrCanonical, excludes = sourcesToCompile)

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)

            if (currentRemoved.any()) {
                anyClassesCompiled = true
                currentRemoved = listOf()
            }
        }

        if (exitCode == ExitCode.OK && isIncrementalDecided) {
            buildDirtyLookupSymbols.addAll(javaFilesProcessor.allChangedSymbols)
        }
        if (artifactFile != null && artifactDifferenceRegistry != null) {
            val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
            val artifactDifference = ArtifactDifference(currentBuildInfo.startTS, dirtyData)
            artifactDifferenceRegistry!!.add(artifactFile!!, artifactDifference)
            logger.kotlinDebug {
                val dirtySymbolsSorted = buildDirtyLookupSymbols.map { it.scope + "#" + it.name }.sorted()
                "Added artifact difference for $artifactFile (ts: ${currentBuildInfo.startTS}): " +
                        "[\n\t${dirtySymbolsSorted.joinToString(",\n\t")}]"
            }
        }

        anyClassesCompiled = anyClassesCompiled || allGeneratedFiles.isNotEmpty()
        caches.close(flush = true)
        logger.kotlinDebug { "flushed incremental caches" }
        processCompilerExitCode(exitCode)
    }

    private data class CompileChangedResults(val exitCode: ExitCode, val generatedFiles: List<GeneratedFile<TargetId>>)

    private fun compileChanged(
            targets: List<TargetId>,
            sourcesToCompile: Set<File>,
            outputDir: File,
            args: K2JVMCompilerArguments,
            getIncrementalCache: (TargetId)->GradleIncrementalCacheImpl,
            lookupTracker: LookupTracker
    ): CompileChangedResults {
        val moduleFile = makeModuleFile(args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = getJavaSourceRoots(),
                classpath = compileClasspath,
                friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val outputItemCollector = OutputItemsCollectorImpl()
        val messageCollector = GradleMessageCollector(logger, outputItemCollector)
        sourceAnnotationsRegistry?.clear()

        try {
            val incrementalCaches = makeIncrementalCachesMap(targets, { listOf<TargetId>() }, getIncrementalCache, { this })
            val compilationCanceledStatus = object : CompilationCanceledStatus {
                override fun checkCanceled() {
                }
            }

            logger.kotlinDebug("compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}")
            logger.kotlinDebug("compiling with classpath: ${compileClasspath.toList().sorted().joinToString()}")
            val compileServices = makeCompileServices(incrementalCaches, lookupTracker, compilationCanceledStatus, sourceAnnotationsRegistry)
            val exitCode = compiler.exec(messageCollector, compileServices, args)
            return CompileChangedResults(
                    exitCode,
                    outputItemCollector.generatedFiles(
                            targets = targets,
                            representativeTarget = targets.first(),
                            getSources = { sourcesToCompile },
                            getOutputDir = { outputDir }))
        }
        finally {
            moduleFile.delete()
        }
    }

    private fun compileNotIncremental(
            sourcesToCompile: List<File>,
            outputDir: File,
            args: K2JVMCompilerArguments
    ): ExitCode {
        logger.kotlinDebug("Removing all kotlin classes in $outputDir")
        // we're free to delete all classes since only we know about that directory
        // todo: can be optimized -- compile and remove only files that were not generated
        listClassFiles(outputDir.canonicalPath).forEach { it.delete() }

        val moduleFile = makeModuleFile(
                args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = getJavaSourceRoots(),
                classpath = compileClasspath,
                friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val messageCollector = GradleMessageCollector(logger)

        sourceAnnotationsRegistry?.clear()
        val services = with (Services.Builder()) {
            sourceAnnotationsRegistry?.let { handler ->
                register(SourceRetentionAnnotationHandler::class.java, handler)
            }
            build()
        }

        try {
            logger.kotlinDebug("compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}")
            logger.kotlinDebug("compiling with classpath: ${compileClasspath.toList().sorted().joinToString()}")
            return compiler.exec(messageCollector, services, args)
        }
        finally {
            moduleFile.delete()
        }
    }

    private fun handleKaptProperties() {
        kaptOptions.annotationsFile?.let { kaptAnnotationsFile ->
            if (incremental) {
                kaptAnnotationsFileUpdater = AnnotationFileUpdater(kaptAnnotationsFile)
            }

            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "output", kaptAnnotationsFile.canonicalPath)
        }

        if (kaptOptions.generateStubs) {
            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "stubs", destinationDir.canonicalPath)
        }

        if (kaptOptions.supportInheritedAnnotations) {
            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "inherited", true.toString())
        }
    }

    private fun getJavaSourceRoots(): Set<File> =
            findRootsForSources(getSource().filter { it.isJavaFile() })

    private fun File.isJavaFile() =
            extension.equals(JavaFileType.INSTANCE.defaultExtension, ignoreCase = true)

    private fun File.isKapt2GeneratedDirectory(): Boolean {
        if (!kapt2GeneratedSourcesDir.isDirectory) return false
        return FileUtil.isAncestor(kapt2GeneratedSourcesDir, this, /* strict = */ false)
    }

    private fun filterOutKapt2Directories(vararg sources: Any?): Array<Any> {
        return sources.flatMap { source ->
            when (source) {
                is File -> if (source.isKapt2GeneratedDirectory()) emptyList<File>() else listOf(source)
                is SourceDirectorySet -> source.srcDirs.filter { !it.isKapt2GeneratedDirectory() }
                else -> emptyList<File>()
            }
        }.toTypedArray()
    }

    // override setSource to track source directory sets and files (for generated android folders)
    override fun setSource(sources: Any?) {
        sourceRoots.clear()
        val sourcesToAdd = filterOutKapt2Directories(sources)
        addSourceRoots(*sourcesToAdd)
        super.setSource(sourcesToAdd.firstOrNull())
    }

    // override source to track source directory sets and files (for generated android folders)
    override fun source(vararg sources: Any?): SourceTask? {
        val sourcesToAdd = filterOutKapt2Directories(*sources)
        addSourceRoots(*sourcesToAdd)
        return super.source(*sourcesToAdd)
    }

    internal fun findRootsForSources(sources: Iterable<File>): Set<File> {
        val resultRoots = HashSet<File>()
        val sourceDirs = sources.mapTo(HashSet()) { it.parentFile }

        for (sourceDir in sourceDirs) {
            for (sourceRoot in sourceRoots) {
                if (FileUtil.isAncestor(sourceRoot, sourceDir, /* strict = */false)) {
                    resultRoots.add(sourceRoot)
                }
            }
        }

        return resultRoots
    }

    private fun addSourceRoots(vararg sources: Any?) {
        for (source in sources) {
            when (source) {
                is SourceDirectorySet -> sourceRoots.addAll(source.srcDirs)
                is File -> sourceRoots.add(source)
            }
        }
    }
}

open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {
    override val compiler = K2JSCompiler()
    private val kotlinOptionsImpl = KotlinJsOptionsImpl()
    override val kotlinOptions: KotlinJsOptions
            get() = kotlinOptionsImpl

    @Suppress("unused")
    val outputFile: String?
        get() = kotlinOptions.outputFile

    init {
        @Suppress("LeakingThis")
        outputs.file(MethodClosure(this, "getOutputFile"))
    }

    override fun populateCompilerArguments(): K2JSCompilerArguments {
        val args = K2JSCompilerArguments().apply { fillDefaultValues() }
        args.libraryFiles = project.configurations.getByName("compile")
                .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                .map { it.canonicalPath }
                .toTypedArray()
        kotlinOptionsImpl.updateArguments(args)
        return args
    }

    override fun callCompiler(args: K2JSCompilerArguments, sources: List<File>, isIncremental: Boolean, modified: List<File>, removed: List<File>) {
        val messageCollector = GradleMessageCollector(logger)
        logger.debug("Calling compiler")
        destinationDir.mkdirs()
        args.freeArgs = args.freeArgs + sources.map { it.absolutePath }

        if (args.outputFile == null) {
            throw GradleException("$name.kotlinOptions.outputFile should be specified.")
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        when (exitCode) {
            ExitCode.OK -> {
                logger.kotlinInfo("Compilation succeeded")
            }
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
            else -> throw GradleException("Unexpected exit code: $exitCode")
        }
    }
}

internal class GradleMessageCollector(val logger: Logger, val outputCollector: OutputItemsCollector? = null) : MessageCollector {
    private var hasErrors = false

    override fun hasErrors() = hasErrors

    override fun clear() {
        // Do nothing
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val text = with(StringBuilder()) {
            append(when (severity) {
                in CompilerMessageSeverity.VERBOSE -> "v"
                in CompilerMessageSeverity.ERRORS -> {
                    hasErrors = true
                    "e"
                }
                CompilerMessageSeverity.INFO -> "i"
                CompilerMessageSeverity.WARNING -> "w"
                else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
            })
            append(": ")

            val (path, line, column) = location
            if (path != null) {
                append(path)
                append(": ")
                if (line > 0 && column > 0) {
                    append("($line, $column): ")
                }
            }

            append(message)

            toString()
        }
        when (severity) {
            in CompilerMessageSeverity.VERBOSE -> logger.debug(text)
            in CompilerMessageSeverity.ERRORS -> logger.error(text)
            CompilerMessageSeverity.INFO -> logger.info(text)
            CompilerMessageSeverity.WARNING -> logger.warn(text)
            else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
        }
        // TODO: consider adding some other way of passing input -> output mapping from compiler, e.g. dedicated service
        if (outputCollector != null && severity == CompilerMessageSeverity.OUTPUT) {
            OutputMessageUtil.parseOutputMessage(message)?.let {
                outputCollector.add(it.sourceFiles, it.outputFile)
            }
        }
    }
}

internal fun Logger.kotlinInfo(message: String) {
    this.info("[KOTLIN] $message")
}

internal fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

internal inline fun Logger.kotlinDebug(message: ()->String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}

internal fun listClassFiles(path: String): Sequence<File> =
        File(path).walk().filter { it.isFile && it.hasClassFileExtension() }

private fun File.hasClassFileExtension(): Boolean =
        extension.toLowerCase() == "class"
