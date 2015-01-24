/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import gnu.trove.THashSet
import org.jetbrains.kotlin.cli.common.KotlinVersion
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.CompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.CompilerRunnerConstants
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings
import org.jetbrains.kotlin.jps.incremental.*
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCacheProvider
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.incremental.java.JavaBuilder
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import java.io.File
import java.util.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.compilerRunner.CompilerRunnerConstants.INTERNAL_ERROR_PREFIX
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunner.runK2JsCompiler
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunner.runK2JvmCompiler
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode.*
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.jps.builders.java.JavaBuilderUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.kotlin.compilerRunner.SimpleOutputItem
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache

public class KotlinBuilder : ModuleLevelBuilder(BuilderCategory.SOURCE_PROCESSOR) {
    class object {
        public val KOTLIN_BUILDER_NAME: String = "Kotlin Builder"

        private val LOG = Logger.getInstance("#org.jetbrains.jps.cmdline.BuildSession")
    }

    override fun getPresentableName() = KOTLIN_BUILDER_NAME

    override fun getCompilableFileExtensions() = arrayListOf("kt")

    override fun build(
            context: CompileContext,
            chunk: ModuleChunk,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            outputConsumer: ModuleLevelBuilder.OutputConsumer
    ): ModuleLevelBuilder.ExitCode {
        val historyLabel = context.getBuilderParameter("history label")
        if (historyLabel != null) {
            LOG.info("Label in local history: $historyLabel")
        }

        val messageCollector = MessageCollectorAdapter(context)
        // Workaround for Android Studio
        if (!JpsUtils.isJsKotlinModule(chunk.representativeTarget()) && !JavaBuilder.IS_ENABLED[context, true]) {
            messageCollector.report(INFO, "Kotlin JPS plugin is disabled", NO_LOCATION)
            return NOTHING_DONE
        }

        val dataManager = context.getProjectDescriptor().dataManager

        if (chunk.getTargets().any { dataManager.getDataPaths().getKotlinCacheVersion(it).isIncompatible() }) {
            chunk.getTargets().forEach { dataManager.getKotlinCache(it).clean() }
            return CHUNK_REBUILD_REQUIRED
        }

        if (!dirtyFilesHolder.hasDirtyFiles() && !dirtyFilesHolder.hasRemovedFiles()
            || !hasKotlinDirtyOrRemovedFiles(dirtyFilesHolder, chunk)) {
            return NOTHING_DONE
        }

        messageCollector.report(INFO, "Kotlin JPS plugin version " + KotlinVersion.VERSION, NO_LOCATION)

        val incrementalCaches = chunk.getTargets().keysToMap { dataManager.getKotlinCache(it) }
        val environment = createCompileEnvironment(incrementalCaches)
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector)
            return ABORT
        }

        val project = context.getProjectDescriptor().getProject()
        val commonArguments = JpsKotlinCompilerSettings.getCommonCompilerArguments(project)
        commonArguments.verbose = true // Make compiler report source to output files mapping

        val allCompiledFiles = getAllCompiledFilesContainer(context)
        val filesToCompile = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder)

        val outputItemCollector = if (JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            compileToJs(chunk, commonArguments, environment, messageCollector, project)
        }
        else {
            compileToJvm(allCompiledFiles, chunk, commonArguments, context, dirtyFilesHolder, environment, filesToCompile, messageCollector)
        }

        if (outputItemCollector == null) {
            return NOTHING_DONE
        }

        val compilationErrors = Utils.ERRORS_DETECTED_KEY[context, false]
        val outputsItemsAndTargets = getOutputItemsAndTargets(chunk, outputItemCollector)

        registerOutputItems(outputConsumer, outputsItemsAndTargets)

        val recompilationDecision: IncrementalCacheImpl.RecompilationDecision
        if (JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            recompilationDecision = IncrementalCacheImpl.RecompilationDecision.DO_NOTHING
        }
        else {
            recompilationDecision = updateKotlinIncrementalCache(compilationErrors, dirtyFilesHolder, incrementalCaches, outputsItemsAndTargets)
        }

        if (compilationErrors) {
            return ABORT
        }

        if (JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            copyJsLibraryFilesIfNeeded(chunk, project)
        }

        if (IncrementalCompilation.ENABLED) {
            if (recompilationDecision == IncrementalCacheImpl.RecompilationDecision.RECOMPILE_ALL) {
                allCompiledFiles.clear()
                val targetsCompletelyMarkedDirty = FSOperations.getTargetsCompletelyMarkedDirty(context)
                FSOperations.markDirtyRecursively(context, chunk)
            }
            if (recompilationDecision == IncrementalCacheImpl.RecompilationDecision.COMPILE_OTHERS) {
                FSOperations.markDirtyRecursively(context, chunk, { file ->
                    KotlinSourceFileCollector.isKotlinSourceFile(file) && file !in allCompiledFiles
                })
            }
            return ADDITIONAL_PASS_REQUIRED
        }

        return OK
    }

    private fun createCompileEnvironment(incrementalCaches: Map<ModuleBuildTarget, IncrementalCache>): CompilerEnvironment {
        val compilerServices = Services.Builder()
                .register(javaClass<IncrementalCacheProvider>(), IncrementalCacheProviderImpl(incrementalCaches))
                .build()

        return CompilerEnvironment.getEnvironmentFor(
                PathUtil.getKotlinPathsForJpsPluginOrJpsTests(),
                { className ->
                    className.startsWith("org.jetbrains.kotlin.load.kotlin.incremental.cache.")
                    || className == "org.jetbrains.kotlin.config.Services"
                },
                compilerServices
        )
    }

    private fun getOutputItemsAndTargets(
            chunk: ModuleChunk,
            outputItemCollector: OutputItemsCollectorImpl
    ): List<Pair<SimpleOutputItem, ModuleBuildTarget>> {
        // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
        val sourceToTarget = HashMap<File, ModuleBuildTarget>()
        if (chunk.getTargets().size() > 1) {
            for (target in chunk.getTargets()) {
                for (file in KotlinSourceFileCollector.getAllKotlinSourceFiles(target)) {
                    sourceToTarget.put(file, target)
                }
            }
        }

        val result = ArrayList<Pair<SimpleOutputItem, ModuleBuildTarget>>()

        val representativeTarget = chunk.representativeTarget()
        for (outputItem in outputItemCollector.getOutputs()) {
            var target: ModuleBuildTarget? = null
            val sourceFiles = outputItem.getSourceFiles()
            if (!sourceFiles.isEmpty()) {
                target = sourceToTarget[sourceFiles.iterator().next()]
            }

            if (target == null) {
                target = representativeTarget
            }

            result.add(Pair(outputItem, target!!))
        }
        return result
    }

    private fun registerOutputItems(outputConsumer: ModuleLevelBuilder.OutputConsumer, outputsItemsAndTargets: List<Pair<SimpleOutputItem, ModuleBuildTarget>>) {
        for ((outputItem, target) in outputsItemsAndTargets) {
            outputConsumer.registerOutputFile(target, outputItem.getOutputFile(), outputItem.getSourceFiles().map { it.getPath() })
        }
    }

    private fun updateKotlinIncrementalCache(
            compilationErrors: Boolean,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            incrementalCaches: Map<ModuleBuildTarget, IncrementalCacheImpl>,
            outputsItemsAndTargets: List<Pair<SimpleOutputItem, ModuleBuildTarget>>
    ): IncrementalCacheImpl.RecompilationDecision {
        if (!IncrementalCompilation.ENABLED) {
            return IncrementalCacheImpl.RecompilationDecision.DO_NOTHING
        }

        for ((target, cache) in incrementalCaches) {
            cache.clearCacheForRemovedFiles(
                    KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target),
                    target.getOutputDir()!!,
                    !compilationErrors
            )
        }

        var recompilationDecision = IncrementalCacheImpl.RecompilationDecision.DO_NOTHING
        for ((outputItem, target) in outputsItemsAndTargets) {
            val newDecision = incrementalCaches[target]!!.saveFileToCache(outputItem.getSourceFiles(), outputItem.getOutputFile())
            recompilationDecision = recompilationDecision.merge(newDecision)
        }
        return recompilationDecision
    }

    // if null is returned, nothing was done
    private fun compileToJs(chunk: ModuleChunk,
                            commonArguments: CommonCompilerArguments,
                            environment: CompilerEnvironment,
                            messageCollector: KotlinBuilder.MessageCollectorAdapter,
                            project: JpsProject
    ): OutputItemsCollectorImpl? {
        val outputItemCollector = OutputItemsCollectorImpl()

        val representativeTarget = chunk.representativeTarget()
        if (chunk.getModules().size() > 1) {
            // We do not support circular dependencies, but if they are present, we do our best should not break the build,
            // so we simply yield a warning and report NOTHING_DONE
            messageCollector.report(WARNING, "Circular dependencies are not supported. "
                                             + "The following JS modules depend on each other: "
                                             + chunk.getModules().map { it.getName() }.joinToString(", ")
                                             + ". "
                                             + "Kotlin is not compiled for these modules", NO_LOCATION)
            return null
        }

        val sourceFiles = KotlinSourceFileCollector.getAllKotlinSourceFiles(representativeTarget)
        if (sourceFiles.isEmpty()) {
            return null
        }

        val outputDir = KotlinBuilderModuleScriptGenerator.getOutputDirSafe(representativeTarget)

        val outputFile = File(outputDir, representativeTarget.getModule().getName() + ".js")
        val libraryFiles = JpsJsModuleUtils.getLibraryFilesAndDependencies(representativeTarget)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(project)
        val k2JsArguments = JpsKotlinCompilerSettings.getK2JsCompilerArguments(project)

        runK2JsCompiler(commonArguments, k2JsArguments, compilerSettings, messageCollector, environment, outputItemCollector, sourceFiles, libraryFiles, outputFile)
        return outputItemCollector
    }

    private fun copyJsLibraryFilesIfNeeded(chunk: ModuleChunk, project: JpsProject) {
        val representativeTarget = chunk.representativeTarget()
        val outputDir = KotlinBuilderModuleScriptGenerator.getOutputDirSafe(representativeTarget)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(project)
        if (compilerSettings.copyJsLibraryFiles) {
            val outputLibraryRuntimeDirectory = File(outputDir, compilerSettings.outputDirectoryForJsLibraryFiles).getAbsolutePath()
            val libraryFilesToCopy = arrayListOf<String>()
            JpsJsModuleUtils.getLibraryFiles(representativeTarget, libraryFilesToCopy)
            LibraryUtils.copyJsFilesFromLibraries(libraryFilesToCopy, outputLibraryRuntimeDirectory)
        }
    }

    // if null is returned, nothing was done
    private fun compileToJvm(allCompiledFiles: MutableSet<File>,
                             chunk: ModuleChunk,
                             commonArguments: CommonCompilerArguments,
                             context: CompileContext,
                             dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
                             environment: CompilerEnvironment,
                             filesToCompile: MultiMap<ModuleBuildTarget, File>,
                             messageCollector: KotlinBuilder.MessageCollectorAdapter
    ): OutputItemsCollectorImpl? {
        val outputItemCollector = OutputItemsCollectorImpl()

        if (chunk.getModules().size() > 1) {
            messageCollector.report(WARNING, "Circular dependencies are only partially supported. "
                                             + "The following modules depend on each other: "
                                             + chunk.getModules().map { it.getName() }.joinToString(", ")
                                             + ". "
                                             + "Kotlin will compile them, but some strange effect may happen", NO_LOCATION)
        }

        allCompiledFiles.addAll(filesToCompile.values())

        val processedTargetsWithRemoved = getProcessedTargetsWithRemovedFilesContainer(context)

        var haveRemovedFiles = false
        for (target in chunk.getTargets()) {
            if (!KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target).isEmpty()) {
                if (processedTargetsWithRemoved.add(target)) {
                    haveRemovedFiles = true
                }
            }
        }

        val moduleFile = KotlinBuilderModuleScriptGenerator.generateModuleDescription(context, chunk, filesToCompile, haveRemovedFiles)
        if (moduleFile == null) {
            // No Kotlin sources found
            return null
        }

        val project = context.getProjectDescriptor().getProject()
        val k2JvmArguments = JpsKotlinCompilerSettings.getK2JvmCompilerArguments(project)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(project)

        runK2JvmCompiler(commonArguments, k2JvmArguments, compilerSettings, messageCollector, environment, moduleFile, outputItemCollector)
        moduleFile.delete()

        return outputItemCollector
    }

    public class MessageCollectorAdapter(private val context: CompileContext) : MessageCollector {

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            var prefix = ""
            if (severity == EXCEPTION) {
                prefix = INTERNAL_ERROR_PREFIX
            }
            context.processMessage(CompilerMessage(
                    CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                    kind(severity),
                    prefix + message + renderLocationIfNeeded(location),
                    location.getPath(),
                    -1, -1, -1,
                    location.getLine().toLong(), location.getColumn().toLong()
            ))
        }

        private fun renderLocationIfNeeded(location: CompilerMessageLocation): String {
            if (location == NO_LOCATION) return ""

            // Sometimes we report errors in JavaScript library stubs, i.e. files like core/javautil.kt
            // IDEA can't find these files, and does not display paths in Messages View, so we add the position information
            // to the error message itself:
            val pathname = "" + location.getPath()
            return if (File(pathname).exists()) "" else " (" + location + ")"
        }

        private fun kind(severity: CompilerMessageSeverity): BuildMessage.Kind {
            return when (severity) {
                INFO -> BuildMessage.Kind.INFO
                ERROR, EXCEPTION -> BuildMessage.Kind.ERROR
                WARNING -> BuildMessage.Kind.WARNING
                LOGGING -> BuildMessage.Kind.PROGRESS
                else -> throw IllegalArgumentException("Unsupported severity: " + severity)
            }
        }

    }
}

private val ALL_COMPILED_FILES_KEY = Key.create<MutableSet<File>>("_all_kotlin_compiled_files_")
private fun getAllCompiledFilesContainer(context: CompileContext): MutableSet<File> {
    var allCompiledFiles = ALL_COMPILED_FILES_KEY.get(context)
    if (allCompiledFiles == null) {
        allCompiledFiles = THashSet(FileUtil.FILE_HASHING_STRATEGY)
        ALL_COMPILED_FILES_KEY.set(context, allCompiledFiles)
    }
    return allCompiledFiles!!
}

private val PROCESSED_TARGETS_WITH_REMOVED_FILES = Key.create<MutableSet<ModuleBuildTarget>>("_processed_targets_with_removed_files_")
private fun getProcessedTargetsWithRemovedFilesContainer(context: CompileContext): MutableSet<ModuleBuildTarget> {
    var set = PROCESSED_TARGETS_WITH_REMOVED_FILES.get(context)
    if (set == null) {
        set = HashSet<ModuleBuildTarget>()
        PROCESSED_TARGETS_WITH_REMOVED_FILES.set(context, set)
    }
    return set!!
}

private fun hasKotlinDirtyOrRemovedFiles(
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        chunk: ModuleChunk
): Boolean {
    if (!KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder).isEmpty()) {
        return true
    }

    return chunk.getTargets().any { !KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, it).isEmpty() }
}
