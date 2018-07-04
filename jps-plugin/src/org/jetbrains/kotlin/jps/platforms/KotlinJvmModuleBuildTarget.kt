/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

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
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangesCollector
import org.jetbrains.kotlin.incremental.IncrementalCompilationComponentsImpl
import org.jetbrains.kotlin.incremental.IncrementalJvmCache
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.updateIncrementalCache
import org.jetbrains.kotlin.jps.build.KotlinBuilder
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

class KotlinJvmModuleBuildTarget(compileContext: CompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<JvmBuildMetaInfo>(compileContext, jpsModuleBuildTarget) {

    override fun createCacheStorage(paths: BuildDataPaths) = JpsIncrementalJvmCache(jpsModuleBuildTarget, paths)

    override val buildMetaInfoFactory
        get() = JvmBuildMetaInfo

    override val buildMetaInfoFileName
        get() = JVM_BUILD_META_INFO_FILE_NAME

    override fun makeServices(
        builder: Services.Builder,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker
    ) {
        super.makeServices(builder, incrementalCaches, lookupTracker, exceptActualTracer)

        with(builder) {
            register(
                IncrementalCompilationComponents::class.java,
                IncrementalCompilationComponentsImpl(
                    incrementalCaches.mapKeys { context.kotlinBuildTargets[it.key]!!.targetId } as Map<TargetId, IncrementalCache>
                )
            )
        }
    }

    override fun compileModuleChunk(
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment
    ): Boolean {
        if (chunk.modules.size > 1) {
            environment.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Circular dependencies are only partially supported. The following modules depend on each other: "
                        + chunk.modules.joinToString(", ") { it.name } + " "
                        + "Kotlin will compile them, but some strange effect may happen"
            )
        }

        val filesSet = dirtyFilesHolder.allDirtyFiles

        val moduleFile = generateModuleDescription(chunk, dirtyFilesHolder)
        if (moduleFile == null) {
            if (KotlinBuilder.LOG.isDebugEnabled) {
                KotlinBuilder.LOG.debug(
                    "Not compiling, because no files affected: " + chunk.targets.joinToString { it.presentableName }
                )
            }

            // No Kotlin sources found
            return false
        }

        val module = chunk.representativeTarget().module

        if (KotlinBuilder.LOG.isDebugEnabled) {
            val totalRemovedFiles = dirtyFilesHolder.allRemovedFilesFiles.size
            KotlinBuilder.LOG.debug("Compiling to JVM ${filesSet.size} files"
                                            + (if (totalRemovedFiles == 0) "" else " ($totalRemovedFiles removed files)")
                                            + " in " + chunk.targets.joinToString { it.presentableName })
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
            if (System.getProperty("kotlin.jps.delete.module.file.after.build") != "false") {
                moduleFile.delete()
            }
        }

        return true
    }

    private fun generateModuleDescription(chunk: ModuleChunk, dirtyFilesHolder: KotlinDirtySourceFilesHolder): File? {
        val builder = KotlinModuleXmlBuilder()

        var hasDirtySources = false

        val targets = chunk.targets.mapNotNull { this.context.kotlinBuildTargets[it] as? KotlinJvmModuleBuildTarget }

        val outputDirs = targets.map { it.outputDir }.toSet()

        for (target in targets) {
            val outputDir = target.outputDir
            val friendDirs = target.friendOutputDirs

            val moduleSources = collectSourcesToCompile(target, dirtyFilesHolder)
            val hasDirtyOrRemovedSources = checkShouldCompileAndLog(target, dirtyFilesHolder, moduleSources)
            if (hasDirtyOrRemovedSources) hasDirtySources = true

            val kotlinModuleId = target.targetId
            builder.addModule(
                kotlinModuleId.name,
                outputDir.absolutePath,
                moduleSources,
                target.findSourceRoots(context),
                target.findClassPathRoots(),
                target.findModularJdkRoot(),
                kotlinModuleId.type,
                isTests,
                // this excludes the output directories from the class path, to be removed for true incremental compilation
                outputDirs,
                friendDirs
            )
        }

        if (!hasDirtySources) return null

        val scriptFile = createTempFileForModuleDesc(chunk)
        FileUtil.writeToFile(scriptFile, builder.asText().toString())
        return scriptFile
    }

    private fun createTempFileForModuleDesc(chunk: ModuleChunk): File {
        val readableSuffix = buildString {
            append(StringUtil.sanitizeJavaIdentifier(chunk.representativeTarget().module.name))
            if (chunk.containsTests()) {
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
        jpsIncrementalCache: JpsIncrementalCache,
        files: List<GeneratedFile>,
        changesCollector: ChangesCollector,
        environment: JpsCompilerEnvironment
    ) {
        super.updateCaches(jpsIncrementalCache, files, changesCollector, environment)

        updateIncrementalCache(files, jpsIncrementalCache as IncrementalJvmCache, changesCollector, null)
    }

    override fun updateChunkMappings(
        chunk: ModuleChunk,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        outputItems: Map<ModuleBuildTarget, Iterable<GeneratedFile>>,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>
    ) {
        val previousMappings = context.projectDescriptor.dataManager.mappings
        val callback = JavaBuilderUtil.getDependenciesRegistrar(context)

        val targetDirtyFiles: Map<ModuleBuildTarget, Set<File>> = chunk.targets.keysToMap {
            val files = HashSet<File>()
            files.addAll(dirtyFilesHolder.getRemovedFiles(it))
            files.addAll(dirtyFilesHolder.getDirtyFiles(it))
            files
        }

        fun getOldSourceFiles(target: ModuleBuildTarget, generatedClass: GeneratedJvmClass): Set<File> {
            val cache = incrementalCaches[target] ?: return emptySet()
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
        JavaBuilderUtil.registerFilesToCompile(context, allCompiled)
        JavaBuilderUtil.registerSuccessfullyCompiled(context, allCompiled)
    }
}