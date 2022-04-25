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
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.model.java.JpsJavaExtensionService
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
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.build.KotlinCompileContext
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalJvmCache
import org.jetbrains.kotlin.jps.model.k2JvmCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
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

class KotlinJvmModuleBuildTarget(kotlinContext: KotlinCompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<JvmBuildMetaInfo>(kotlinContext, jpsModuleBuildTarget) {

    override val isIncrementalCompilationEnabled: Boolean
        get() = IncrementalCompilation.isEnabledForJvm()

    override fun createCacheStorage(paths: BuildDataPaths) =
        JpsIncrementalJvmCache(jpsModuleBuildTarget, paths, kotlinContext.fileToPathConverter)

    override val buildMetaInfoFactory
        get() = JvmBuildMetaInfo

    override val buildMetaInfoFileName
        get() = JVM_BUILD_META_INFO_FILE_NAME

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
        exceptActualTracer: ExpectActualTracker,
        inlineConstTracker: InlineConstTracker,
        enumWhenTracker: EnumWhenTracker
    ) {
        super.makeServices(builder, incrementalCaches, lookupTracker, exceptActualTracer, inlineConstTracker, enumWhenTracker)

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
        environment: JpsCompilerEnvironment
    ): Boolean {
        require(chunk.representativeTarget == this)

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
                moduleFile
            )
        } finally {
            if (System.getProperty(DELETE_MODULE_FILE_PROPERTY) != "false") {
                moduleFile.delete()
            }
        }

        return true
    }

    override fun registerOutputItems(outputConsumer: ModuleLevelBuilder.OutputConsumer, outputItems: List<GeneratedFile>) {
        if (kotlinContext.isInstrumentationEnabled) {
            val (classFiles, nonClassFiles) = outputItems.partition { it is GeneratedJvmClass }
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
                target.findSourceRoots(dirtyFilesHolder.context),
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

    private fun findSourceRoots(context: CompileContext): List<JvmSourceRoot> {
        val roots = context.projectDescriptor.buildRootIndex.getTargetRoots(jpsModuleBuildTarget, context)
        val result = mutableListOf<JvmSourceRoot>()
        for (root in roots) {
            val file = root.rootFile
            val prefix = root.packagePrefix
            if (Files.exists(file.toPath())) {
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

        updateIncrementalCache(files, jpsIncrementalCache as IncrementalJvmCache, changesCollector, null)
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

            val name = previousMappings.getName(className.internalName)
            return previousMappings.getClassSources(name)?.toSet() ?: emptySet()
        }

        for ((target, outputs) in outputItems) {
            for (output in outputs) {
                if (output !is GeneratedJvmClass) continue

                val sourceFiles = FileCollectionFactory.createCanonicalFileSet()
                sourceFiles.addAll(getOldSourceFiles(target, output))
                sourceFiles.removeAll(targetDirtyFiles[target] ?: emptySet())
                sourceFiles.addAll(output.sourceFiles)

                // process inlineConstTracker
                for (sourceFile: File in sourceFiles) {
                    processInlineConstTracker(inlineConstTracker, sourceFile, output, callback)
                    processEnumWhenTracker(enumWhenTracker, sourceFile, output, callback)
                }

                callback.associate(
                    FileUtil.toSystemIndependentName(output.outputFile.canonicalPath),
                    sourceFiles.map { FileUtil.toSystemIndependentName(it.canonicalPath) },
                    ClassReader(output.outputClass.fileContents)
                )
            }
        }

        val allCompiled = dirtyFilesHolder.allDirtyFiles
        JavaBuilderUtil.registerFilesToCompile(localContext, allCompiled)
        JavaBuilderUtil.registerSuccessfullyCompiled(localContext, allCompiled)
    }

    private fun processInlineConstTracker(inlineConstTracker: InlineConstTrackerImpl, sourceFile: File, output: GeneratedJvmClass, callback: Callbacks.Backend) {
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

    private fun processEnumWhenTracker(enumWhenTracker: EnumWhenTrackerImpl, sourceFile: File, output: GeneratedJvmClass, callback: Callbacks.Backend) {
        val enumFqNameClasses = enumWhenTracker.whenExpressionFilePathToEnumClassMap[sourceFile.path]?.map { "$it.*" } ?: return
        callback.registerImports(output.outputClass.className.internalName, listOf(), enumFqNameClasses)
    }
}