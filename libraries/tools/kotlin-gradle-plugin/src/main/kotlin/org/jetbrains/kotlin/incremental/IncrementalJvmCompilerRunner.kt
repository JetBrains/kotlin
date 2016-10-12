package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.annotation.SourceAnnotationsRegistry
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.util.io.PersistentEnumeratorBase
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifference
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import org.jetbrains.kotlin.incremental.snapshots.FileCollectionDiff
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.util.*

internal class IncrementalJvmCompilerRunner(
        workingDir: File,
        private var kaptAnnotationsFileUpdater: AnnotationFileUpdater?,
        private val artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider?,
        private val sourceAnnotationsRegistry: SourceAnnotationsRegistry?,
        private val javaSourceRoots: Set<File>,
        private val kapt2GeneratedSourcesDir: File,
        private val artifactFile: File?,
        private val cacheVersions: List<CacheVersion>,
        private val reporter: IncReporter
) {
    var anyClassesCompiled: Boolean = false
            private set
    private val cacheDirectory = File(workingDir, CACHES_DIR_NAME)
    private val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)
    private val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)

    fun compile(
            allKotlinSources: List<File>,
            changedFiles: ChangedFiles,
            args: K2JVMCompilerArguments,
            messageCollector: MessageCollector
    ): ExitCode {
        val targetId = TargetId(name = args.moduleName, type = "java-production")
        var caches = IncrementalCachesManager(targetId, cacheDirectory, File(args.destination))

        return try {
            val javaFilesProcessor = ChangedJavaFilesProcessor()
            val compilationMode = calculateSourcesToCompile(javaFilesProcessor, caches, changedFiles, args.classpathAsList)
            compileIncrementally(args, caches, javaFilesProcessor, allKotlinSources, targetId, compilationMode, messageCollector)
        }
        catch (e: PersistentEnumeratorBase.CorruptedException) {
            caches.clean()
            artifactDifferenceRegistryProvider?.clean()

            reporter.report { "Caches are corrupted. Rebuilding. $e" }
            // try to rebuild
            val javaFilesProcessor = ChangedJavaFilesProcessor()
            caches = IncrementalCachesManager(targetId, cacheDirectory, args.destinationAsFile)
            compileIncrementally(args, caches, javaFilesProcessor, allKotlinSources, targetId, CompilationMode.Rebuild, messageCollector)
        }
    }

    private data class CompileChangedResults(val exitCode: ExitCode, val generatedFiles: List<GeneratedFile<TargetId>>)

    private sealed class CompilationMode {
        class Incremental(val dirtyFiles: Set<File>) : CompilationMode()
        object Rebuild : CompilationMode()
    }

    private fun calculateSourcesToCompile(
            javaFilesProcessor: ChangedJavaFilesProcessor,
            caches: IncrementalCachesManager,
            changedFiles: ChangedFiles,
            classpath: Iterable<File>
    ): CompilationMode {
        fun rebuild(reason: ()->String): CompilationMode {
            reporter.report {"Non-incremental compilation will be performed: ${reason()}"}
            caches.clean()
            dirtySourcesSinceLastTimeFile.delete()
            return CompilationMode.Rebuild
        }

        if (changedFiles !is ChangedFiles.Known) return rebuild {"inputs' changes are unknown (first or clean build)"}

        val removedClassFiles = changedFiles.removed.filter(File::isClassFile)
        if (removedClassFiles.any()) return rebuild {"Removed class files: ${reporter.pathsAsString(removedClassFiles)}"}

        val modifiedClassFiles = changedFiles.modified.filter(File::isClassFile)
        if (modifiedClassFiles.any()) return rebuild {"Modified class files: ${reporter.pathsAsString(modifiedClassFiles)}"}

        val classpathSet = classpath.toHashSet()
        val modifiedClasspathEntries = changedFiles.modified.filter {it in classpathSet}
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
        reporter.report { "Last Kotlin Build info -- $lastBuildInfo" }
        val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo)
        if (classpathChanges !is ChangesEither.Known) {
            return rebuild {"could not get changes from modified classpath entries: ${reporter.pathsAsString(modifiedClasspathEntries)}"}
        }
        val javaFilesDiff = FileCollectionDiff(
                newOrModified = changedFiles.modified.filter(File::isJavaFile),
                removed = changedFiles.removed.filter(File::isJavaFile))
        val javaFilesChanges = javaFilesProcessor.process(javaFilesDiff)
        val affectedJavaSymbols = when (javaFilesChanges) {
            is ChangesEither.Known -> javaFilesChanges.lookupSymbols
            is ChangesEither.Unknown -> return rebuild {"Could not get changes for java files"}
        }

        val dirtyFiles = HashSet<File>(with(changedFiles) {modified.size + removed.size})
        with(changedFiles) {
            modified.asSequence() + removed.asSequence()
        }.forEach {if (it.isKotlinFile()) dirtyFiles.add(it)}

        val lookupSymbols = HashSet<LookupSymbol>()
        lookupSymbols.addAll(affectedJavaSymbols)
        lookupSymbols.addAll(classpathChanges.lookupSymbols)

        if (lookupSymbols.any()) {
            val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
            dirtyFiles.addAll(dirtyFilesFromLookups)
        }

        val dirtyClassesFqNames = classpathChanges.fqNames.flatMap {withSubtypes(it, listOf(caches.incrementalCache))}
        if (dirtyClassesFqNames.any()) {
            val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassesFqNames, reporter)
            dirtyFiles.addAll(dirtyFilesFromFqNames)
        }

        if (dirtySourcesSinceLastTimeFile.exists()) {
            val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter(File::exists)
            if (files.isNotEmpty()) {
                reporter.report {"Source files added since last compilation: ${reporter.pathsAsString(files)}"}
            }

            dirtyFiles.addAll(files)
        }

        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun getClasspathChanges(
            modifiedClasspath: List<File>,
            lastBuildInfo: BuildInfo?
    ): ChangesEither {
        if (modifiedClasspath.isEmpty()) {
            reporter.report {"No classpath changes"}
            return ChangesEither.Known()
        }
        if (artifactDifferenceRegistryProvider == null) {
            reporter.report {"No artifact history provider"}
            return ChangesEither.Unknown()
        }

        val lastBuildTS = lastBuildInfo?.startTS
        if (lastBuildTS == null) {
            reporter.report {"Could not determine last build timestamp"}
            return ChangesEither.Unknown()
        }

        val symbols = HashSet<LookupSymbol>()
        val fqNames = HashSet<FqName>()
        for (file in modifiedClasspath) {
            val diffs = artifactDifferenceRegistryProvider.withRegistry(reporter) {artifactDifferenceRegistry ->
                artifactDifferenceRegistry[file]
            }
            if (diffs == null) {
                reporter.report {"Could not get changes for file: $file"}
                return ChangesEither.Unknown()
            }

            val (beforeLastBuild, afterLastBuild) = diffs.partition {it.buildTS < lastBuildTS}
            if (beforeLastBuild.isEmpty()) {
                reporter.report {"No known build preceding timestamp $lastBuildTS for file $file"}
                return ChangesEither.Unknown()
            }

            afterLastBuild.forEach {
                symbols.addAll(it.dirtyData.dirtyLookupSymbols)
                fqNames.addAll(it.dirtyData.dirtyClassesFqNames)
            }
        }

        return ChangesEither.Known(symbols, fqNames)
    }

    private fun compileIncrementally(
            args: K2JVMCompilerArguments,
            caches: IncrementalCachesManager,
            javaFilesProcessor: ChangedJavaFilesProcessor,
            allKotlinSources: List<File>,
            targetId: TargetId,
            compilationMode: CompilationMode,
            messageCollector: MessageCollector
    ): ExitCode {
        val allGeneratedFiles = hashSetOf<GeneratedFile<TargetId>>()
        val dirtySources: MutableList<File>

        when (compilationMode) {
            is CompilationMode.Incremental -> {
                dirtySources = allKotlinSources.filterTo(ArrayList()) { it in compilationMode.dirtyFiles }
                args.classpathAsList += args.destinationAsFile
            }
            is CompilationMode.Rebuild -> {
                dirtySources = allKotlinSources.toMutableList()
                // there is no point in updating annotation file since all files will be compiled anyway
                kaptAnnotationsFileUpdater = null
            }
            else -> throw IllegalStateException("Unknown CompilationMode ${compilationMode.javaClass}")
        }

        @Suppress("NAME_SHADOWING")
        var compilationMode = compilationMode

        reporter.report { "Artifact to register difference for: $artifactFile" }
        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis())
        BuildInfo.write(currentBuildInfo, lastBuildInfoFile)
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()

        var exitCode = ExitCode.OK
        while (dirtySources.any()) {
            val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
            val outdatedClasses = caches.incrementalCache.classesBySources(dirtySources)
            caches.incrementalCache.markOutputClassesDirty(dirtySources)
            caches.incrementalCache.removeClassfilesBySources(dirtySources)

            val (sourcesToCompile, removedKotlinSources) = dirtySources.partition(File::exists)
            if (sourcesToCompile.isNotEmpty()) {
                reporter.report { "compile iteration: ${reporter.pathsAsString(sourcesToCompile)}" }
            }

            val text = sourcesToCompile.map { it.canonicalPath }.joinToString(separator = System.getProperty("line.separator"))
            dirtySourcesSinceLastTimeFile.writeText(text)

            val compilerOutput = compileChanged(listOf(targetId), sourcesToCompile.toSet(), args, { caches.incrementalCache }, lookupTracker, messageCollector)
            exitCode = compilerOutput.exitCode

            if (exitCode == ExitCode.OK) {
                dirtySourcesSinceLastTimeFile.delete()
                kaptAnnotationsFileUpdater?.updateAnnotations(outdatedClasses)
            } else {
                kaptAnnotationsFileUpdater?.revert()
                break
            }

            val generatedClassFiles = compilerOutput.generatedFiles
            allGeneratedFiles.addAll(generatedClassFiles)
            val compilationResult = updateIncrementalCaches(listOf(targetId), generatedClassFiles,
                    compiledWithErrors = exitCode != ExitCode.OK,
                    getIncrementalCache = { caches.incrementalCache })

            caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)

            val generatedJavaFiles = kapt2GeneratedSourcesDir.walk().filter { it.isJavaFile() }.toList()
            val generatedJavaFilesDiff = caches.incrementalCache.compareAndUpdateFileSnapshots(generatedJavaFiles)

            if (compilationMode is CompilationMode.Rebuild) {
                artifactFile?.let { artifactFile ->
                    artifactDifferenceRegistryProvider?.withRegistry(reporter) { registry ->
                        registry.remove(artifactFile)
                    }
                }
                break
            }

            val (dirtyLookupSymbols, dirtyClassFqNames) = compilationResult.getDirtyData(listOf(caches.incrementalCache), reporter)
            val generatedJavaFilesChanges = javaFilesProcessor.process(generatedJavaFilesDiff)
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()
            val dirtyKotlinFilesFromJava = when (generatedJavaFilesChanges) {
                is ChangesEither.Unknown -> {
                    reporter.report { "Could not get changes for generated java files, recompiling all kotlin" }
                    compilationMode = CompilationMode.Rebuild
                    allKotlinSources.toSet()
                }
                is ChangesEither.Known -> {
                    mapLookupSymbolsToFiles(caches.lookupCache, generatedJavaFilesChanges.lookupSymbols, reporter, excludes = compiledInThisIterationSet)
                }
                else -> throw IllegalStateException("Unknown ChangesEither implementation: $generatedJavaFiles")
            }

            with (dirtySources) {
                clear()
                addAll(dirtyKotlinFilesFromJava)
                addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = compiledInThisIterationSet))
                addAll(mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassFqNames, reporter, excludes = compiledInThisIterationSet))
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)

            anyClassesCompiled = anyClassesCompiled || generatedClassFiles.isNotEmpty() || removedKotlinSources.isNotEmpty()
        }

        if (exitCode == ExitCode.OK && compilationMode is CompilationMode.Incremental) {
            buildDirtyLookupSymbols.addAll(javaFilesProcessor.allChangedSymbols)
        }
        if (artifactFile != null && artifactDifferenceRegistryProvider != null) {
            val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
            val artifactDifference = ArtifactDifference(currentBuildInfo.startTS, dirtyData)
            artifactDifferenceRegistryProvider.withRegistry(reporter) {registry ->
                registry.add(artifactFile, artifactDifference)
            }
            reporter.report {
                val dirtySymbolsSorted = buildDirtyLookupSymbols.map { it.scope + "#" + it.name }.sorted()
                "Added artifact difference for $artifactFile (ts: ${currentBuildInfo.startTS}): " +
                        "[\n\t${dirtySymbolsSorted.joinToString(",\n\t")}]"
            }
        }

        artifactDifferenceRegistryProvider?.withRegistry(reporter) {
            it.flush(memoryCachesOnly = true)
        }
        caches.close(flush = true)
        reporter.report { "flushed incremental caches" }

        if (exitCode == ExitCode.OK) {
            sourceAnnotationsRegistry?.flush()
            cacheVersions.forEach { it.saveIfNeeded() }
        }

        return exitCode
    }

    private fun compileChanged(
            targets: List<TargetId>,
            sourcesToCompile: Set<File>,
            args: K2JVMCompilerArguments,
            getIncrementalCache: (TargetId)->GradleIncrementalCacheImpl,
            lookupTracker: LookupTracker,
            messageCollector: MessageCollector
    ): CompileChangedResults {
        val compiler = K2JVMCompiler()
        val outputDir = args.destinationAsFile
        val classpath = args.classpathAsList
        val moduleFile = makeModuleFile(args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = classpath,
                friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val outputItemCollector = OutputItemsCollectorImpl()
        @Suppress("NAME_SHADOWING")
        val messageCollector = MessageCollectorWrapper(messageCollector, outputItemCollector)
        sourceAnnotationsRegistry?.clear()

        try {
            val incrementalCaches = makeIncrementalCachesMap(targets, { listOf<TargetId>() }, getIncrementalCache, { this })
            val compilationCanceledStatus = object : CompilationCanceledStatus {
                override fun checkCanceled() {
                }
            }

            reporter.report { "compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}" }
            reporter.report { "compiling with classpath: ${classpath.toList().sorted().joinToString()}" }
            val compileServices = makeCompileServices(incrementalCaches, lookupTracker, compilationCanceledStatus, sourceAnnotationsRegistry)
            val exitCode = compiler.exec(messageCollector, compileServices, args)
            return CompileChangedResults(
                    exitCode,
                    outputItemCollector.generatedFiles(
                            targets = targets,
                            representativeTarget = targets.first(),
                            getSources = {sourcesToCompile},
                            getOutputDir = {outputDir}))
        }
        finally {
            moduleFile.delete()
        }
    }

    private class MessageCollectorWrapper(
            private val delegate: MessageCollector,
            private val outputCollector: OutputItemsCollector
    ) : MessageCollector by delegate {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            // TODO: consider adding some other way of passing input -> output mapping from compiler, e.g. dedicated service
            OutputMessageUtil.parseOutputMessage(message)?.let {
                outputCollector.add(it.sourceFiles, it.outputFile)
            }
            delegate.report(severity, message, location)
        }
    }

    companion object {
        const val CACHES_DIR_NAME = "caches"
        const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
        const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
    }
}

internal var K2JVMCompilerArguments.destinationAsFile: File
        get() = File(destination)
        set(value) { destination = value.path }

internal var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath.split(File.pathSeparator).map(::File)
    set(value) { classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path }) }