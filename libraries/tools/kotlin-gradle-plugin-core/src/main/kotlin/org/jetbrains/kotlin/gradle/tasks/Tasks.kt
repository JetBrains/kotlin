package org.jetbrains.kotlin.gradle.tasks

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.GradleException
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
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
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.com.intellij.lang.Language
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File
import java.util.*

const val DEFAULT_ANNOTATIONS = "org.jebrains.kotlin.gradle.defaultAnnotations"
const val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"
const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val CACHES_DIR_NAME = "caches"
const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
const val USING_EXPERIMENTAL_INCREMENTAL_MESSAGE = "Using experimental kotlin incremental compilation"

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractCompile() {
    abstract protected val compiler: CLICompiler<T>
    abstract protected fun createBlankArgs(): T
    open protected fun beforeCompileHook(args: T) {
    }
    open protected fun afterCompileHook(args: T) {
    }
    abstract protected fun populateTargetSpecificArgs(args: T)

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    var incremental: Boolean = false
    var kotlinOptions: T = createBlankArgs()
    var compilerCalled: Boolean = false
    // TODO: consider more reliable approach (see usage)
    var anyClassesCompiled: Boolean = false
    var friendTaskName: String? = null
    var javaOutputDir: File? = null

    private val loggerInstance = Logging.getLogger(this.javaClass)
    override fun getLogger() = loggerInstance

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
        val args = createBlankArgs()
        val sources = getKotlinSources()
        if (sources.isEmpty()) {
            logger.warn("No Kotlin files found, skipping Kotlin compiler task")
            return
        }

        populateCommonArgs(args)
        populateTargetSpecificArgs(args)
        compilerCalled = true
        callCompiler(args, sources, inputs.isIncremental, modified, removed)
    }

    private fun getKotlinSources(): List<File> = (getSource() as Iterable<File>).filter { it.isKotlinFile() }

    protected fun File.isKotlinFile(): Boolean =
            FilenameUtils.isExtension(name.toLowerCase(), listOf("kt", "kts"))

    private fun populateSources(args:T, sources: List<File>) {
        args.freeArgs = sources.map { it.absolutePath }
    }

    private fun populateCommonArgs(args: T) {
        args.suppressWarnings = kotlinOptions.suppressWarnings
        args.verbose = kotlinOptions.verbose
        args.version = kotlinOptions.version
        args.noInline = kotlinOptions.noInline
    }

    protected open fun callCompiler(args: T, sources: List<File>, isIncremental: Boolean, modified: List<File>, removed: List<File>) {
        populateSources(args, sources)

        val messageCollector = GradleMessageCollector(logger)
        logger.debug("Calling compiler")
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
        }
    }

}

open class KotlinCompile() : AbstractKotlinCompile<K2JVMCompilerArguments>() {
    override val compiler = K2JVMCompiler()
    override fun createBlankArgs(): K2JVMCompilerArguments = K2JVMCompilerArguments()

    private val sourceRoots = HashSet<File>()

    // lazy because name is probably not available when constructor is called
    private val taskBuildDirectory: File by lazy { File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name) }
    private val cacheDirectory: File by lazy { File(taskBuildDirectory, CACHES_DIR_NAME) }
    private val dirtySourcesSinceLastTimeFile: File by lazy { File(taskBuildDirectory, DIRTY_SOURCES_FILE_NAME) }
    private val cacheVersions by lazy {
        listOf(normalCacheVersion(taskBuildDirectory),
               experimentalCacheVersion(taskBuildDirectory),
               dataContainerCacheVersion(taskBuildDirectory),
               gradleCacheVersion(taskBuildDirectory))
    }

    private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null
    val kaptOptions = KaptOptions()
    val pluginOptions = CompilerPluginOptions()

    override fun populateTargetSpecificArgs(args: K2JVMCompilerArguments) {
        // show kotlin compiler where to look for java source files
//        args.freeArgs = (args.freeArgs + getJavaSourceRoots().map { it.getAbsolutePath() }).toSet().toList()
        logger.kotlinDebug("args.freeArgs = ${args.freeArgs}")

        if (StringUtils.isEmpty(kotlinOptions.classpath)) {
            args.classpath = classpath.filter({ it != null && it.exists() }).joinToString(File.pathSeparator)
            logger.kotlinDebug("args.classpath = ${args.classpath}")
        }

        if (args.destination?.isNotBlank() ?: false) {
            // TODO: fix if needed
            logger.warn("Setting kotlinOptions.destination is not supported in gradle")
        }

        logger.kotlinDebug("destinationDir = $destinationDir")

        val extraProperties = extensions.extraProperties
        args.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        logger.kotlinDebug("args.pluginClasspaths = ${args.pluginClasspaths.joinToString(File.pathSeparator)}")
        handleKaptProperties()
        args.pluginOptions = pluginOptions.arguments.toTypedArray()
        logger.kotlinDebug("args.pluginOptions = ${args.pluginOptions.joinToString(File.pathSeparator)}")

        args.noStdlib = true
        args.jdkHome = kotlinOptions.jdkHome
        args.noJdk = kotlinOptions.noJdk
        args.noInline = kotlinOptions.noInline
        args.noOptimize = kotlinOptions.noOptimize
        args.noCallAssertions = kotlinOptions.noCallAssertions
        args.noParamAssertions = kotlinOptions.noParamAssertions
        args.moduleName = kotlinOptions.moduleName ?: extraProperties.getOrNull<String>("defaultModuleName")
        args.languageVersion = kotlinOptions.languageVersion
        args.jvmTarget = kotlinOptions.jvmTarget

        fun addFriendPathForTestTask(friendKotlinTaskName: String) {
            val friendTask = project.getTasksByName(friendKotlinTaskName, /* recursive = */false).firstOrNull() as? KotlinCompile ?: return
            args.friendPaths = arrayOf(friendTask.javaOutputDir!!.absolutePath)
            args.moduleName = friendTask.kotlinOptions.moduleName ?: friendTask.extensions.extraProperties.getOrNull<String>("defaultModuleName")
            logger.kotlinDebug("java destination directory for production = ${friendTask.javaOutputDir}")
        }

        logger.kotlinDebug { "friendTaskName = $friendTaskName" }
        friendTaskName?.let { addFriendPathForTestTask(it) }
        logger.kotlinDebug("args.moduleName = ${args.moduleName}")

        fun dumpPaths(files: Iterable<File>): String =
            "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"

        logger.kotlinDebug { "$name source roots: ${dumpPaths(sourceRoots)}" }
        logger.kotlinDebug { "$name java source roots: ${dumpPaths(getJavaSourceRoots())}" }
    }

    override fun callCompiler(args: K2JVMCompilerArguments, sources: List<File>, isIncrementalRequested: Boolean, modified: List<File>, removed: List<File>) {

        fun projectRelativePath(f: File) = f.toRelativeString(project.projectDir)

        if (incremental) {
            // TODO: consider other ways to pass incremental flag to compiler/builder
            System.setProperty("kotlin.incremental.compilation", "true")
            // TODO: experimental should be removed as soon as it becomes standard
            System.setProperty("kotlin.incremental.compilation.experimental", "true")
        }

        val targetType = "java-production"
        val moduleName = args.moduleName
        val targets = listOf(TargetId(moduleName, targetType))
        val outputDir = destinationDir
        val caches = hashMapOf<TargetId, GradleIncrementalCacheImpl>()
        val lookupStorage = LookupStorage(File(cacheDirectory, "lookups"))
        val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
        var currentRemoved = removed.filter { it.isKotlinFile() }
        val allGeneratedFiles = hashSetOf<GeneratedFile<TargetId>>()
        val logAction = { logStr: String -> logger.kotlinInfo(logStr) }

        fun getOrCreateIncrementalCache(target: TargetId): GradleIncrementalCacheImpl {
            val cacheDir = File(cacheDirectory, "increCache.${target.name}")
            cacheDir.mkdirs()
            return GradleIncrementalCacheImpl(targetDataRoot = cacheDir, targetOutputDir = outputDir, target = target)
        }

        fun getIncrementalCache(it: TargetId) = caches.getOrPut(it, { getOrCreateIncrementalCache(it) })

        fun PsiClass.findLookupSymbols(): Iterable<LookupSymbol> {
            val fqn = qualifiedName.orEmpty()
            return listOf(LookupSymbol(name.orEmpty(), if (fqn == name) "" else fqn.removeSuffix("." + name!!))) +
                    methods.map { LookupSymbol(it.name, fqn) } +
                    fields.map { LookupSymbol(it.name.orEmpty(), fqn) } +
                    innerClasses.flatMap { it.findLookupSymbols() }
        }

        fun dirtyLookupSymbolsFromModifiedJavaFiles(): List<LookupSymbol> {
            val modifiedJavaFiles = modified.filter { it.isJavaFile() }
            return (if (modifiedJavaFiles.any()) {
                val rootDisposable = Disposer.newDisposable()
                val configuration = CompilerConfiguration()
                val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
                val project = environment.project
                val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
                modifiedJavaFiles.flatMap {
                    val javaFile = psiFileFactory.createFileFromText(it.nameWithoutExtension, Language.findLanguageByID("JAVA")!!, it.readText())
                    if (javaFile is PsiJavaFile)
                        javaFile.classes.flatMap { it.findLookupSymbols() }
                    else listOf()
                }
            } else listOf())
        }

        fun dirtyKotlinSourcesFromGradle(): MutableSet<File> {
            // TODO: handle classpath changes similarly - compare with cashed version (likely a big change, may be costly, some heuristics could be considered)
            val modifiedKotlinFiles = modified.filter { it.isKotlinFile() }.toMutableSet()
            val lookupSymbols = dirtyLookupSymbolsFromModifiedJavaFiles()
                    // TODO: add dirty lookups from modified kotlin files to reduce number of steps needed

            if (lookupSymbols.any()) {
                val kotlinModifiedFilesSet = modifiedKotlinFiles.toHashSet()
                val dirtyFilesFromLookups = mapLookupSymbolsToFiles(lookupStorage, lookupSymbols, logAction, ::projectRelativePath, excludes = kotlinModifiedFilesSet)
                modifiedKotlinFiles.addAll(dirtyFilesFromLookups)
            }

            return modifiedKotlinFiles
        }

        fun isClassPathChanged(): Boolean {
            // TODO: that doesn't look to wise - join it first and then split here, consider storing it somewhere in between
            val classpath = args.classpath.split(File.pathSeparator).map { File(it) }.toHashSet()
            val changedClasspath = modified.filter { classpath.contains(it) }
            return changedClasspath.any()
        }

        fun calculateSourcesToCompile(): Pair<Set<File>, Boolean> {
            if (!incremental
                || !isIncrementalRequested
                // TODO: more precise will be not to rebuild unconditionally on classpath changes, but retrieve lookup info and try to find out which sources are affected by cp changes
                || isClassPathChanged()
                // so far considering it not incremental TODO: store java files in the cache and extract removed symbols from it here
                || removed.any { it.isJavaFile() || it.hasClassFileExtension() }
                || modified.any { it.hasClassFileExtension() }
                || cacheVersions.any { it.checkVersion() != CacheVersion.Action.DO_NOTHING }
            ) {
                logger.kotlinInfo(if (!isIncrementalRequested) "clean caches on rebuild" else "classpath changed, rebuilding all kotlin files")
                targets.forEach { getIncrementalCache(it).clean() }
                lookupStorage.clean()
                dirtySourcesSinceLastTimeFile.delete()
                return Pair(sources.toSet(), false)
            }

            val dirtyFiles = dirtyKotlinSourcesFromGradle()
            if (dirtySourcesSinceLastTimeFile.exists()) {
                val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter { it.exists() }
                if (files.isNotEmpty()) {
                    logger.kotlinDebug { "Source files added since last compilation: $files" }
                }

                dirtyFiles.addAll(files)
            }

            return Pair(dirtyFiles, true)
        }

        fun cleanupOnError() {
            val filesToDelete = allGeneratedFiles.map { it.outputFile }
            logger.kotlinInfo("deleting output on error: ${filesToDelete.joinToString()}")
            filesToDelete.forEach { it.delete() }
        }

        fun processCompilerExitCode(exitCode: ExitCode) {
            if (exitCode != ExitCode.OK) {
                cleanupOnError()
            }

            lookupStorage.flush(false)
            lookupStorage.close()
            caches.values.forEach { it.flush(false); it.close() }
            logger.debug("flushed incremental caches")

            when (exitCode) {
                ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
                ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
                ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
                ExitCode.OK -> logger.kotlinInfo("Compilation succeeded")
            }
        }

        if (!incremental) {
            anyClassesCompiled = true
            processCompilerExitCode(compileNotIncremental(sources, outputDir, args))
            return
        }

        logger.warn(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
        anyClassesCompiled = false
        // TODO: decide what to do if no files are considered dirty - rebuild or skip the module
        var (sourcesToCompile, isIncrementalDecided) = calculateSourcesToCompile()

        if (isIncrementalDecided) {
            // TODO: process as list here, merge into string later
            args.classpath = args.classpath + File.pathSeparator + outputDir.absolutePath
        }
        else {
            // there is no point in updating annotation file since all files will be compiled anyway
            kaptAnnotationsFileUpdater = null
        }

        var exitCode = ExitCode.OK
        while (sourcesToCompile.any() || currentRemoved.any()) {
            val removedAndModified = (sourcesToCompile + currentRemoved).toList()
            val outdatedClasses = targets.flatMap { getIncrementalCache(it).classesBySources(removedAndModified) }

            targets.forEach { getIncrementalCache(it).let {
                it.markOutputClassesDirty(removedAndModified)
                it.removeClassfilesBySources(removedAndModified)
            }}

            // can be empty if only removed sources are present
            if (sourcesToCompile.isNotEmpty()) {
                logger.kotlinInfo("compile iteration: ${sourcesToCompile.joinToString { projectRelativePath(it) }}")
            }

            val (existingSource, nonExistingSource) = sourcesToCompile.partition { it.isFile }
            assert(nonExistingSource.isEmpty()) { "Trying to compile removed files: ${nonExistingSource.map(::projectRelativePath)}" }

            val text = existingSource.map { it.canonicalPath }.joinToString(separator = System.getProperty("line.separator"))
            dirtySourcesSinceLastTimeFile.writeText(text)

            val compilerOutput = compileChanged(targets, existingSource.toSet(), outputDir, args, ::getIncrementalCache, lookupTracker)
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
            val compilationResult = updateIncrementalCaches(targets, compilerOutput.generatedFiles,
                                                            compiledWithErrors = exitCode != ExitCode.OK,
                                                            getIncrementalCache = { caches[it]!! })

            lookupStorage.update(lookupTracker, sourcesToCompile, currentRemoved)
            cacheVersions.forEach { it.saveIfNeeded() }

            if (!isIncrementalDecided) break

            val (dirtyLookupSymbols, dirtyClassFqNames) = compilationResult.getDirtyData(caches.values, logAction)
            sourcesToCompile = mapLookupSymbolsToFiles(lookupStorage, dirtyLookupSymbols, logAction, ::projectRelativePath, excludes = sourcesToCompile) +
                               mapClassesFqNamesToFiles(caches.values, dirtyClassFqNames, logAction, ::projectRelativePath, excludes = sourcesToCompile)

            if (currentRemoved.any()) {
                currentRemoved = listOf()
            }
        }

        anyClassesCompiled = allGeneratedFiles.isNotEmpty()
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
        val moduleFile = makeModuleFile(args.moduleName, isTest = false, outputDir = outputDir, sourcesToCompile = sourcesToCompile, javaSourceRoots = getJavaSourceRoots(), classpath = classpath, friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val outputItemCollector = OutputItemsCollectorImpl()
        val messageCollector = GradleMessageCollector(logger, outputItemCollector)

        try {
            val incrementalCaches = makeIncrementalCachesMap(targets, { listOf<TargetId>() }, getIncrementalCache, { this })
            val compilationCanceledStatus = object : CompilationCanceledStatus {
                override fun checkCanceled() {
                }
            }

            logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")
            val exitCode = compiler.exec(messageCollector, makeCompileServices(incrementalCaches, lookupTracker, compilationCanceledStatus), args)
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

        val moduleFile = makeModuleFile(args.moduleName, isTest = false, outputDir = outputDir, sourcesToCompile = sourcesToCompile, javaSourceRoots = getJavaSourceRoots(), classpath = classpath, friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val messageCollector = GradleMessageCollector(logger)

        try {
            logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")
            return compiler.exec(messageCollector, Services.EMPTY, args)
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

            if (kaptAnnotationsFile.exists()) kaptAnnotationsFile.delete()
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

    // override setSource to track source directory sets and files (for generated android folders)
    override fun setSource(source: Any?) {
        sourceRoots.clear()
        addSourceRoot(source)
        super.setSource(source)
    }

    // override source to track source directory sets and files (for generated android folders)
    override fun source(vararg sources: Any?): SourceTask? {
        sources.forEach { addSourceRoot(it) }
        return super.source(sources)
    }

    fun findRootsForSources(sources: Iterable<File>): Set<File> {
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

    private fun addSourceRoot(source: Any?) {
        when (source) {
            is SourceDirectorySet -> {
                sourceRoots.addAll(source.srcDirs)
            }
            is File -> {
                sourceRoots.add(source)
            }
        }
    }
}

open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>() {
    override val compiler = K2JSCompiler()

    override fun createBlankArgs(): K2JSCompilerArguments {
        val args = K2JSCompilerArguments()
        args.libraryFiles = arrayOf<String>()  // defaults to null
        args.metaInfo = true
        args.kjsm = true
        return args
    }

    fun addLibraryFiles(vararg fs: String) {
        kotlinOptions.libraryFiles += fs
    }

    fun addLibraryFiles(vararg fs: File) {
        val strs = fs.map { it.path }.toTypedArray()
        addLibraryFiles(*strs)
    }

    val outputFile: String?
            get() = kotlinOptions.outputFile

    val sourceMapDestinationDir: File
            get() = File(outputFile).let { if (it.isDirectory) it else it.parentFile!! }

    val sourceMap: Boolean
            get() = kotlinOptions.sourceMap

    init {
        outputs.file(MethodClosure(this, "getOutputFile"))
    }

    override fun populateTargetSpecificArgs(args: K2JSCompilerArguments) {
        args.noStdlib = true
        args.outputFile = outputFile
        args.outputPrefix = kotlinOptions.outputPrefix
        args.outputPostfix = kotlinOptions.outputPostfix
        args.metaInfo = kotlinOptions.metaInfo
        args.kjsm = kotlinOptions.kjsm

        val kotlinJsLibsFromDependencies =
                project.configurations.getByName("compile")
                        .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                        .map { it.absolutePath }

        args.libraryFiles = kotlinOptions.libraryFiles + kotlinJsLibsFromDependencies
        args.target = kotlinOptions.target
        args.sourceMap = kotlinOptions.sourceMap

        if (args.outputFile == null) {
            throw GradleException("$name.kotlinOptions.outputFile should be specified.")
        }

        val outputDir = File(args.outputFile).let { if (it.isDirectory) it else it.parentFile!! }
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw GradleException("Failed to create output directory ${outputDir} or one of its ancestors")
            }
        }

        logger.debug("$name set libraryFiles to ${args.libraryFiles.joinToString(",")}")
        logger.debug("$name set outputFile to ${args.outputFile}")

    }
}

private fun <T: Any> ExtraPropertiesExtension.getOrNull(id: String): T? {
    try {
        @Suppress("UNCHECKED_CAST")
        return get(id) as? T
    }
    catch (e: ExtraPropertiesExtension.UnknownPropertyException) {
        return null
    }
}

class GradleMessageCollector(val logger: Logger, val outputCollector: OutputItemsCollector? = null) : MessageCollector {
    private var hasErrors = false

    override fun hasErrors() = hasErrors

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

inline
internal fun Logger.kotlinDebug(message: ()->String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}

internal fun listClassFiles(path: String): Sequence<File> =
        File(path).walk().filter { it.isFile && it.hasClassFileExtension() }

private fun File.hasClassFileExtension(): Boolean =
        extension.toLowerCase() == "class"
