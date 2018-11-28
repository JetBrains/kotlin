/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.URLUtil
import gnu.trove.THashSet
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.java.JavaBuilderUtil
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsSdkDependency
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
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
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

private const val JVM_BUILD_META_INFO_FILE_NAME = "jvm-build-meta-info.txt"

class KotlinJvmModuleBuildTarget(kotlinContext: KotlinCompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<JvmBuildMetaInfo>(kotlinContext, jpsModuleBuildTarget) {

    override val isIncrementalCompilationEnabled: Boolean
        get() = IncrementalCompilation.isEnabledForJvm()

    override fun createCacheStorage(paths: BuildDataPaths) = JpsIncrementalJvmCache(jpsModuleBuildTarget, paths)

    override val buildMetaInfoFactory
        get() = JvmBuildMetaInfo

    override val buildMetaInfoFileName
        get() = JVM_BUILD_META_INFO_FILE_NAME

    override fun makeServices(
        builder: Services.Builder,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker
    ) {
        super.makeServices(builder, incrementalCaches, lookupTracker, exceptActualTracer)

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
            builder.addModule(
                kotlinModuleId.name,
                outputDir.absolutePath,
                sources.allFiles,
                target.findSourceRoots(dirtyFilesHolder.context),
                target.findClassPathRoots(),
                sources.crossCompiledFiles,
                target.findModularJdkRoot(),
                kotlinModuleId.type,
                isTests,
                // this excludes the output directories from the class path, to be removed for true incremental compilation
                outputDirs,
                friendDirs
            )
        }

        if (!hasDirtySources) return null

        val scriptFile = createTempFileForChunkModuleDesc()
        FileUtil.writeToFile(scriptFile, builder.asText().toString())
        return scriptFile
    }

    private fun createTempFileForChunkModuleDesc(): File {
        val readableSuffix = buildString {
            append(StringUtil.sanitizeJavaIdentifier(chunk.representativeTarget.module.name))
            if (chunk.containsTests) {
                append("-test")
            }
        }
        val dir = System.getProperty("kotlin.jps.dir.for.module.files")?.let { File(it) }?.takeIf { it.isDirectory }
        return try {
            File.createTempFile("kjps", readableSuffix + ".script.xml", dir)
        } catch (e: IOException) {
            // sometimes files cannot be created, because file name is too long (Windows, Mac OS)
            // see https://bugs.openjdk.java.net/browse/JDK-8148023
            try {
                File.createTempFile("kjps", ".script.xml", dir)
            } catch (e: IOException) {
                val message = buildString {
                    append("Could not create module file when building chunk $chunk")
                    if (dir != null) {
                        append(" in dir $dir")
                    }
                }
                throw RuntimeException(message, e)
            }
        }
    }

    private fun findClassPathRoots(): Collection<File> {
        return allDependencies.classes().roots.filter { file ->
            if (!file.exists()) {
                val extension = file.extension

                // Don't filter out files, we want to report warnings about absence through the common place
                if (extension != "class" && extension != "jar") {
                    return@filter false
                }
            }

            true
        }
    }

    private fun findModularJdkRoot(): File? {
        // List of paths to JRE modules in the following format:
        // jrt:///Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home!/java.base
        val urls = JpsJavaExtensionService.dependencies(module)
            .satisfying { dependency -> dependency is JpsSdkDependency }
            .classes().urls

        val url = urls.firstOrNull { it.startsWith(StandardFileSystems.JRT_PROTOCOL_PREFIX) } ?: return null

        return File(url.substringAfter(StandardFileSystems.JRT_PROTOCOL_PREFIX).substringBeforeLast(URLUtil.JAR_SEPARATOR))
    }

    private fun findSourceRoots(context: CompileContext): List<JvmSourceRoot> {
        val roots = context.projectDescriptor.buildRootIndex.getTargetRoots(jpsModuleBuildTarget, context)
        val result = ContainerUtil.newArrayList<JvmSourceRoot>()
        for (root in roots) {
            val file = root.rootFile
            val prefix = root.packagePrefix
            if (file.exists()) {
                result.add(JvmSourceRoot(file, if (prefix.isEmpty()) null else prefix))
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
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>
    ) {
        val previousMappings = localContext.projectDescriptor.dataManager.mappings
        val callback = JavaBuilderUtil.getDependenciesRegistrar(localContext)

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

                val sourceFiles = THashSet(FileUtil.FILE_HASHING_STRATEGY)
                sourceFiles.addAll(getOldSourceFiles(target, output))
                sourceFiles.removeAll(targetDirtyFiles[target] ?: emptySet())
                sourceFiles.addAll(output.sourceFiles)

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
}