/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.jps.targets

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.FileCollectionFactory
import com.intellij.util.io.URLUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.java.JavaBuilderUtil
import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import org.jetbrains.jps.builders.java.dependencyView.Callbacks.Backend
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleDependency
import org.jetbrains.jps.model.module.JpsSdkDependency
import org.jetbrains.jps.service.JpsServiceManager
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmBuildMetaInfo
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBuildToolsApiCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBtaCompilationUnit
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBtaIncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.build.KotlinCompileContext
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalJvmCache
import org.jetbrains.kotlin.jps.model.k2JvmCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import org.jetbrains.kotlin.jps.statistic.JpsBuilderMetricReporter
import org.jetbrains.kotlin.jps.targets.impl.LookupUsageRegistrar
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.ClassReader
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths

private const val JVM_BUILD_META_INFO_FILE_NAME = "jvm-build-meta-info.txt"

/**
 * Sits next to the `kotlin` directory JPS uses for its own Kotlin caches, under the target's JPS data root, so that
 * both are discarded together when JPS drops the target's build data.
 */
private const val BTA_CACHES_DIRECTORY_NAME = "kotlin-bta"

class KotlinJvmModuleBuildTarget(kotlinContext: KotlinCompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<JvmBuildMetaInfo>(kotlinContext, jpsModuleBuildTarget) {

    override val isIncrementalCompilationEnabled: Boolean
        get() = IncrementalCompilation.isEnabledForJvm()

    override val isIncrementalCompilationDelegatedToCompiler: Boolean
        get() = JpsBuildToolsApiCompilerRunner.isEnabled && isIncrementalCompilationEnabled

    /**
     * Switching this off is what actually stops JPS's Kotlin bookkeeping: [JpsIncrementalCache] instances are only
     * handed out for targets that have caches, so every site that reads one skips itself, including the round of
     * `markAdditionalFilesForInitialRound` that would otherwise call into the legacy compiler runner.
     */
    override val hasCaches: Boolean
        get() = !isIncrementalCompilationDelegatedToCompiler

    override fun createCacheStorage(paths: BuildDataPaths) =
        JpsIncrementalJvmCache(jpsModuleBuildTarget, paths, kotlinContext.icContext)

    override val compilerArgumentsFileName
        get() = JVM_BUILD_META_INFO_FILE_NAME

    override val buildMetaInfo: JvmBuildMetaInfo
        get() = JvmBuildMetaInfo()

    override val targetId: TargetId
        get() {
            val moduleName = module.k2JvmCompilerArguments.moduleName
            return if (moduleName != null) TargetId(moduleName, jpsModuleBuildTarget.targetType.typeId)
            else super.targetId
        }

    override fun makeServices(
        builder: Services.Builder,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        inlineConstTracker: InlineConstTracker,
        enumWhenTracker: EnumWhenTracker,
        importTracker: ImportTracker
    ) {
        super.makeServices(builder, incrementalCaches, lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker, importTracker)

        with(builder) {
            register(
                IncrementalCompilationComponents::class.java,
                @Suppress("UNCHECKED_CAST")
                IncrementalCompilationComponentsImpl(
                    incrementalCaches.mapKeys { it.key.targetId } as Map<TargetId, IncrementalCache>
                )
            )
        }
    }

    override fun compileModuleChunk(
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment,
        buildMetricReporter: JpsBuilderMetricReporter?
    ): Boolean {
        require(chunk.representativeTarget == this)

        if (JpsBuildToolsApiCompilerRunner.isEnabled) {
            return compileModuleWithBuildToolsApi(commonArguments, dirtyFilesHolder, environment)
        }

        if (chunk.targets.size > 1) {
            environment.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Circular dependencies are only partially supported. " +
                        "The following modules depend on each other: ${chunk.presentableShortName}. " +
                        "Kotlin will compile them, but some strange effect may happen"
            )
        }

        val filesSet = dirtyFilesHolder.allDirtyFiles

        val moduleFile = generateChunkModuleDescription(dirtyFilesHolder)
        if (moduleFile == null) {
            if (KotlinBuilder.LOG.isDebugEnabled) {
                KotlinBuilder.LOG.debug(
                    "Not compiling, because no files affected: " + chunk.presentableShortName
                )
            }

            // No Kotlin sources found
            return false
        }

        val module = chunk.representativeTarget.module

        if (KotlinBuilder.LOG.isDebugEnabled) {
            val totalRemovedFiles = dirtyFilesHolder.allRemovedFilesFiles.size
            KotlinBuilder.LOG.debug(
                "Compiling to JVM ${filesSet.size} files"
                        + (if (totalRemovedFiles == 0) "" else " ($totalRemovedFiles removed files)")
                        + " in " + chunk.presentableShortName
            )
        }

        try {
            val compilerRunner = JpsKotlinCompilerRunner()
            compilerRunner.runK2JvmCompiler(
                commonArguments,
                module.k2JvmCompilerArguments,
                module.kotlinCompilerSettings,
                environment,
                moduleFile,
                buildMetricReporter
            )
        } finally {
            if (System.getProperty(DELETE_MODULE_FILE_PROPERTY) != "false") {
                moduleFile.delete()
            }
        }

        return true
    }

    /**
     * Compiles this module through the Build Tools API instead of generating a `module.xml` and talking to the compile
     * daemon. Gated on [JpsBuildToolsApiCompilerRunner.USE_BUILD_TOOLS_API_PROPERTY].
     *
     * Only Kotlin-only, non-multiplatform modules without module dependencies and in-process execution are supported
     * so far. Every unsupported case is reported as `ERROR` rather than degrading silently: that sets
     * `Utils.ERRORS_DETECTED_KEY` and makes the build fail instead of producing output that quietly differs from the
     * legacy path.
     *
     * Incremental compilation is run by the compiler rather than by JPS, see [JpsBtaIncrementalCompilation].
     */
    private fun compileModuleWithBuildToolsApi(
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment,
    ): Boolean {
        if (chunk.targets.size > 1) {
            environment.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "The Build Tools API path does not support circular module dependencies. " +
                        "The following modules depend on each other: ${chunk.presentableShortName}"
            )
            return false
        }

        val moduleDependencies = compileScopeModuleDependencies()
        if (isIncrementalCompilationEnabled && moduleDependencies.isNotEmpty()) {
            // The compiler is told to treat the classpath as unchanged, so a change in a dependency would be missed.
            environment.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "The Build Tools API path does not support incremental compilation of a module that depends on other " +
                        "modules. Module '${targetId.name}' depends on: ${moduleDependencies.joinToString()}."
            )
            return false
        }

        val sources = collectSourcesToCompile(dirtyFilesHolder)
        if (!sources.logFiles()) {
            // No Kotlin sources found
            return false
        }

        // Deliberately checked against every source of the module, not against the ones JPS marked dirty: whether the
        // module is multiplatform does not depend on which of its files happen to have changed.
        if (this.sources.values.any { it.isCrossCompiled }) {
            // Sources included from a common source set have to be passed as `-Xcommon-sources` so that `expect`
            // declarations can be matched with their `actual`s. Compiling them as ordinary sources would fail with
            // confusing errors, so reject them outright instead.
            environment.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "The Build Tools API path does not support multiplatform modules yet. " +
                        "Module '${targetId.name}' includes sources of a common source set."
            )
            return false
        }

        // The compiler decides for itself what to recompile, so it needs every source of the module even on an
        // incremental build; `sources.allFiles` would only be the files JPS marked dirty.
        val allSources = preprocessSources(this.sources.values.map { it.file })

        val compilationUnit = JpsBtaCompilationUnit(
            moduleName = targetId.name,
            sources = allSources,
            outputDir = outputDir,
            // Output directories of the module and of its dependencies are excluded from the classpath so that stale
            // binaries of a previous build cannot leak in, exactly as KotlinModuleXmlBuilder.processClasspath does.
            // With incremental compilation they stay, also as in KotlinModuleXmlBuilder.processClasspath.
            classpath = when {
                isIncrementalCompilationEnabled -> findClassPathRoots()
                else -> findClassPathRoots() - chunk.targets.map { it.outputDir }.toSet()
            },
            friendDirs = friendOutputDirs,
            javaSourceRoots = findJavaSourceRoots(dirtyFilesHolder.context),
            modularJdkRoot = findModularJdkRoot(),
            incremental = incrementalCompilation(dirtyFilesHolder, allSources, environment),
        )

        return JpsBuildToolsApiCompilerRunner(dirtyFilesHolder.context, environment).compile(
            compilationUnit,
            commonArguments,
            module.k2JvmCompilerArguments,
            module.kotlinCompilerSettings,
        )
    }

    /**
     * Describes the incremental run to the compiler, or `null` when incremental compilation is off and it should
     * simply compile everything it was given.
     *
     * The changed files come from JPS, which is the only party that knows what the user edited. The compiler expands
     * that seed into the actual compile set itself, using the caches under [BTA_CACHES_DIRECTORY_NAME].
     */
    private fun incrementalCompilation(
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        allSources: List<File>,
        environment: JpsCompilerEnvironment,
    ): JpsBtaIncrementalCompilation? {
        if (!isIncrementalCompilationEnabled) return null

        val targetFiles = dirtyFilesHolder.byTarget[jpsModuleBuildTarget]
        // `dirty` is keyed by the normalized path but holds the file as JPS found it, and `sources` is built from the
        // same walk. Taking the values keeps both lists comparable by `File.equals`; a modified file that does not
        // match any known source is treated as removed by the compiler and silently never compiled.
        val modifiedFiles = preprocessSources(targetFiles?.dirty?.values?.map { it.file }.orEmpty())
        val removedFiles = targetFiles?.removed?.toList().orEmpty()

        val unknownFiles = modifiedFiles - allSources.toSet()
        if (unknownFiles.isNotEmpty()) {
            environment.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Internal error in the Build Tools API path: files were reported as changed but are not among the " +
                        "sources of module '${targetId.name}': ${unknownFiles.joinToString()}"
            )
            return null
        }

        return JpsBtaIncrementalCompilation(
            workingDir = kotlinContext.dataPaths.getTargetDataRootDir(jpsModuleBuildTarget)
                .resolve(BTA_CACHES_DIRECTORY_NAME).toFile(),
            sourcesChanges = SourcesChanges.Known(modifiedFiles, removedFiles),
            forceRecompilation = isChunkRebuilding(dirtyFilesHolder.context),
        )
    }

    /**
     * Mirrors `KotlinBuilder.doBuild`'s own `isChunkRebuilding`, which is not reachable from here. Covers *Rebuild
     * Project* as well as the cache version and compiler argument changes that make JPS throw its caches away.
     */
    private fun isChunkRebuilding(context: CompileContext): Boolean =
        JavaBuilderUtil.isForcedRecompilationAllJavaModules(context) ||
                kotlinContext.rebuildAfterCacheVersionChanged[jpsModuleBuildTarget] == true

    /**
     * Modules this one depends on within the compile classpath, by name. Scope-filtered exactly as
     * [org.jetbrains.kotlin.jps.build.KotlinChunk.calculateTargetDependencies] does, so that a runtime-only or
     * test-only dependency of a production target does not count.
     */
    private fun compileScopeModuleDependencies(): List<String> {
        val jpsJava = JpsJavaExtensionService.getInstance()
        val compileKind = JpsJavaClasspathKind.compile(isTests)
        return module.dependenciesList.dependencies
            .filterIsInstance<JpsModuleDependency>()
            .filter { jpsJava.getDependencyExtension(it)?.scope?.isIncludedIn(compileKind) == true }
            .map { it.moduleReference.moduleName }
    }

    override fun registerOutputItems(outputConsumer: ModuleLevelBuilder.OutputConsumer, outputItems: List<GeneratedFile>) {
        if (kotlinContext.isInstrumentationEnabled) {
            val [classFiles, nonClassFiles] = outputItems.partition { it is GeneratedJvmClass }
            super.registerOutputItems(outputConsumer, nonClassFiles)

            for (output in classFiles) {
                val bytes = output.outputFile.readBytes()
                val binaryContent = BinaryContent(bytes)
                val compiledClass = CompiledClass(output.outputFile, output.sourceFiles, ClassReader(bytes).className, binaryContent)
                outputConsumer.registerCompiledClass(jpsModuleBuildTarget, compiledClass)
            }
        } else {
            super.registerOutputItems(outputConsumer, outputItems)
        }
    }

    private fun generateChunkModuleDescription(dirtyFilesHolder: KotlinDirtySourceFilesHolder): File? {
        val builder = KotlinModuleXmlBuilder()

        var hasDirtySources = false

        val targets = chunk.targets

        val outputDirs = targets.map { it.outputDir }.toSet()

        for (target in targets) {
            target as KotlinJvmModuleBuildTarget

            val outputDir = target.outputDir
            val friendDirs = target.friendOutputDirs

            val sources = target.collectSourcesToCompile(dirtyFilesHolder)

            if (sources.logFiles()) {
                hasDirtySources = true
            }

            val kotlinModuleId = target.targetId
            val allFiles = sources.allFiles
            val commonSourceFiles = sources.crossCompiledFiles

            builder.addModule(
                kotlinModuleId.name,
                outputDir.absolutePath,
                preprocessSources(allFiles),
                target.findJavaSourceRoots(dirtyFilesHolder.context),
                target.findClassPathRoots(),
                preprocessSources(commonSourceFiles),
                target.findModularJdkRoot(),
                kotlinModuleId.type,
                isTests,
                // this excludes the output directories from the class path, to be removed for true incremental compilation
                outputDirs,
                friendDirs,
                IncrementalCompilation.isEnabledForJvm()
            )
        }

        if (!hasDirtySources) return null

        val scriptFile = createTempFileForChunkModuleDesc()
        FileUtil.writeToFile(scriptFile, builder.asText().toString())
        return scriptFile
    }

    /**
     * Internal API for source level code preprocessors.
     *
     * Currently used in https://plugins.jetbrains.com/plugin/13355-spot-profiler-for-java
     */
    interface SourcesPreprocessor {
        /**
         * Preprocess some sources and return path to the resulting file.
         * This function should be pure and should return the same output for given input
         * (required for incremental compilation).
         */
        fun preprocessSources(srcFiles: List<File>): List<File>
    }

    fun preprocessSources(srcFiles: List<File>): List<File> {
        var result = srcFiles
        JpsServiceManager.getInstance().getExtensions(SourcesPreprocessor::class.java).forEach {
            result = it.preprocessSources(result)
        }
        return result
    }

    private fun createTempFileForChunkModuleDesc(): File {
        val readableSuffix = buildString {
            append(StringUtil.sanitizeJavaIdentifier(chunk.representativeTarget.module.name))
            if (chunk.containsTests) {
                append("-test")
            }
        }
        val dir = System.getProperty("kotlin.jps.dir.for.module.files")?.let { Paths.get(it) }?.takeIf { Files.isDirectory(it) }

        fun createTempFile(dir: Path?, prefix: String?, suffix: String?): Path =
            if (dir != null) Files.createTempFile(dir, prefix, suffix) else Files.createTempFile(prefix, suffix)

        fun throwException(e: Exception, dir: Path?, message: String? = null): Path {
            val msg = buildString {
                append("Could not create module file when building chunk $chunk")
                if (dir != null) {
                    append(" in dir $dir")
                }
                if (message != null) append(message)
            }
            throw RuntimeException(msg, e)
        }

        return try {
            createTempFile(dir, "kjps", "$readableSuffix.script.xml")
        } catch (e: NoSuchFileException) {
            val parentDir = File(e.file).parentFile
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    val message = if (dir == null) {
                        val tmpPath = System.getProperty("java.io.tmpdir", null).trim().ifEmpty { null }
                        "java.io.tmpdir is set to $tmpPath and it does not exist. Attempt to create it failed with exception"
                    } else {
                        "kotlin.jps.dir.for.module.files is set to $dir and it does not exist. " +
                                "Attempt to create it failed with exception"
                    }
                    throwException(e, dir, message)
                }
            }

            try {
                createTempFile(dir, "kjps", ".script.xml")
            } catch (e: IOException) {
                throwException(e, dir)
            }
        } catch (e: IOException) {
            // sometimes files cannot be created, because file name is too long (Windows, Mac OS)
            // see https://bugs.openjdk.java.net/browse/JDK-8148023
            try {
                createTempFile(dir, "kjps", ".script.xml")
            } catch (e: IOException) {
                throwException(e, dir)
            }
        }.toFile()
    }

    private fun findClassPathRoots(): Collection<File> = allDependencies.classes().roots.filter { file ->
        val path = file.toPath()

        if (Files.notExists(path)) {
            val extension = path.fileName?.toString()?.substringAfterLast('.', "") ?: ""

            // Don't filter out files, we want to report warnings about absence through the common place
            if (extension != "class" && extension != "jar") {
                return@filter false
            }
        }

        true
    }

    private fun findModularJdkRoot(): File? {
        // List of paths to JRE modules in the following format:
        // jrt:///Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home!/java.base
        val urls = JpsJavaExtensionService.dependencies(module)
            .satisfying { dependency -> dependency is JpsSdkDependency }
            .classes().urls

        val url = urls.firstOrNull { it.startsWith(URLUtil.JRT_PROTOCOL + URLUtil.SCHEME_SEPARATOR) } ?: return null

        return File(url.substringAfter(URLUtil.JRT_PROTOCOL + URLUtil.SCHEME_SEPARATOR).substringBeforeLast(URLUtil.JAR_SEPARATOR))
    }

    private fun findJavaSourceRoots(context: CompileContext): List<JvmSourceRoot> {
        val roots = context.projectDescriptor.buildRootIndex.getTargetRoots(jpsModuleBuildTarget, context)
        val result = mutableListOf<JvmSourceRoot>()
        for (root in roots) {
            val filePath = root.rootFile
            val file = filePath.toFile()
            val prefix = root.packagePrefix
            if (Files.exists(filePath) && (Files.isDirectory(filePath) || file.extension == "java")) {
                result.add(JvmSourceRoot(file, prefix.ifEmpty { null }))
            }
        }
        return result
    }

    override fun updateCaches(
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        jpsIncrementalCache: JpsIncrementalCache,
        files: List<GeneratedFile>,
        changesCollector: ChangesCollector,
        environment: JpsCompilerEnvironment
    ) {
        super.updateCaches(dirtyFilesHolder, jpsIncrementalCache, files, changesCollector, environment)

        updateIncrementalCache(files, jpsIncrementalCache as IncrementalJvmCache, changesCollector, null, null)
    }

    override val globalLookupCacheId: String
        get() = "jvm"

    override fun updateChunkMappings(
        localContext: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        outputItems: Map<ModuleBuildTarget, Iterable<GeneratedFile>>,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        environment: JpsCompilerEnvironment
    ) {
        val previousMappings = localContext.projectDescriptor.dataManager.mappings
        val callback = JavaBuilderUtil.getDependenciesRegistrar(localContext)
        val inlineConstTracker = environment.services[InlineConstTracker::class.java] as InlineConstTrackerImpl
        val enumWhenTracker = environment.services[EnumWhenTracker::class.java] as EnumWhenTrackerImpl
        val importTracker = environment.services[ImportTracker::class.java] as ImportTrackerImpl

        val targetDirtyFiles: Map<ModuleBuildTarget, Set<File>> = chunk.targets.keysToMap {
            val files = HashSet<File>()
            files.addAll(dirtyFilesHolder.getRemovedFiles(it))
            files.addAll(dirtyFilesHolder.getDirtyFiles(it).keys)
            files
        }

        fun getOldSourceFiles(target: ModuleBuildTarget, generatedClass: GeneratedJvmClass): Set<File> {
            val cache = incrementalCaches[kotlinContext.targetsBinding[target]] ?: return emptySet()
            cache as JpsIncrementalJvmCache

            val className = generatedClass.outputClass.className
            if (!cache.isMultifileFacade(className)) return emptySet()

            // In case of graph implementation of JPS
            if (KotlinBuilder.useDependencyGraph || previousMappings == null) return emptySet()

            val name = previousMappings.getName(className.internalName)
            return previousMappings.getClassSources(name).toSet()
        }

        if (KotlinBuilder.useDependencyGraph) {
            LookupUsageRegistrar().processLookupTracker(
                environment.services[LookupTracker::class.java],
                callback,
                environment.messageCollector
            )
        }
        for ([target, outputs] in outputItems) {
            for (output in outputs) {
                if (output !is GeneratedJvmClass) continue

                val sourceFiles = FileCollectionFactory.createCanonicalFileSet()
                sourceFiles.addAll(getOldSourceFiles(target, output))
                sourceFiles.removeAll(targetDirtyFiles[target] ?: emptySet())
                sourceFiles.addAll(output.sourceFiles)

                // process trackers
                for (sourceFile: File in sourceFiles) {
                    processInlineConstTracker(inlineConstTracker, sourceFile, output, callback)
                    processBothEnumWhenAndImportTrackers(enumWhenTracker, importTracker, sourceFile, output, callback)
                }

                callback.associate(
                    FileUtil.toSystemIndependentName(output.outputFile.normalize().absolutePath),
                    sourceFiles.map { FileUtil.toSystemIndependentName(it.normalize().absolutePath) },
                    ClassReader(output.outputClass.fileContents)
                )
            }
        }
        // important: in jps-dependency-graph you can't register additional dependencies after [callback.associate].
    }

    private fun processInlineConstTracker(inlineConstTracker: InlineConstTrackerImpl, sourceFile: File, output: GeneratedJvmClass, callback: Backend) {
        val cRefs = inlineConstTracker.inlineConstMap[sourceFile.path]?.mapNotNull { cRef: ConstantRef ->
            val descriptor = when (cRef.constType) {
                "Byte" -> "B"
                "Short" -> "S"
                "Int" -> "I"
                "Long" -> "J"
                "Float" -> "F"
                "Double" -> "D"
                "Boolean" -> "Z"
                "Char" -> "C"
                "String" -> "Ljava/lang/String;"
                else -> null
            } ?: return@mapNotNull null
            Callbacks.createConstantReference(cRef.owner, cRef.name, descriptor)
        } ?: return

        val className = output.outputClass.className.internalName
        callback.registerConstantReferences(className, cRefs)
    }

    private fun processBothEnumWhenAndImportTrackers(enumWhenTracker: EnumWhenTrackerImpl, importTracker: ImportTrackerImpl, sourceFile: File, output: GeneratedJvmClass, callback: Backend) {
        val enumFqNameClasses = enumWhenTracker.whenExpressionFilePathToEnumClassMap[sourceFile.path]?.map { "$it.*" }
        val importedFqNames = importTracker.filePathToImportedFqNamesMap[sourceFile.path]
        if (enumFqNameClasses == null && importedFqNames == null) return

        callback.registerImports(output.outputClass.className.internalName, importedFqNames ?: listOf(), enumFqNameClasses ?: listOf())
    }
}
