/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliRunnerIsolatedClassLoadersService
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.gradle.plugin.konan.registerIsolatedClassLoadersServiceIfAbsent
import org.jetbrains.kotlin.gradle.plugin.konan.runKonanTool
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.nativeDistribution.NativeDistribution
import org.jetbrains.kotlin.nativeDistribution.asNativeDistribution
import javax.inject.Inject

private abstract class KonanCacheAction : WorkAction<KonanCacheAction.Parameters> {
    interface Parameters : WorkParameters {
        val isolatedClassLoaderService: Property<KonanCliRunnerIsolatedClassLoadersService>
        val compilerClasspath: ConfigurableFileCollection
        val args: ListProperty<String>
    }

    override fun execute() = with(parameters) {
        isolatedClassLoaderService.get().getClassLoader(compilerClasspath.files).runKonanTool(
                toolName = "konanc",
                args = args.get(),
                useArgFile = true,
        )
    }
}

@CacheableTask
open class KonanCacheTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val klib: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    val target: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    val makePerFileCache: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    // Ideally, this would be an output, because that's what we give the compiler and it infers
    // cache name all by itself. But this will break for platform libs which all share the same
    // `cacheDirectory` and we trust the compiler to write into individual folders.
    @get:Internal("used to compute outputDirectory")
    val cacheDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:Internal("used to compute outputDirectory")
    val cacheName: Property<String> = objectFactory.property(String::class.java)

    private fun fullCacheDirectory(perFile: Provider<Boolean>) =
            cacheDirectory.dir(perFile.zip(cacheName) { perFile, name -> NativeDistribution.cacheDirectoryName(name, perFile) })

    @get:OutputDirectory
    val outputDirectory: Provider<Directory> = fullCacheDirectory(makePerFileCache)

    @get:OutputDirectory // This will be deleted, not created.
    protected val alternativeCacheDirectory: Provider<Directory> = fullCacheDirectory(makePerFileCache.map { !it })

    @get:Internal("Depends upon the compiler classpath, native libraries (used by codegen) and konan.properties (compilation flags + dependencies)")
    val compilerDistributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    private val compilerDistribution = compilerDistributionRoot.asNativeDistribution()

    @get:Classpath
    protected val compilerClasspath: Provider<FileCollection> = compilerDistribution.map { it.compilerClasspath }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val codegenLibs: Provider<Directory> = compilerDistribution.map { it.nativeLibs }

    @get:Input
    val withOptimizations: Property<Boolean> = objectFactory.property(Boolean::class.java)

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val konanProperties: Provider<RegularFile> = compilerDistribution.map { it.konanProperties }

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    @TaskAction
    fun compile() {
        val outDir = outputDirectory.get().asFile
        outDir.prepareAsOutput()

        // Native compiler will not build per-file cache, when monolithic cache exists.
        // Before running the compiler, delete both kinds of caches. The cache that will
        // be built, was deleted above; the other cache is deleted here.
        // See https://youtrack.jetbrains.com/issue/KT-86726
        alternativeCacheDirectory.get().asFile.deleteRecursively()

        val klibFile = klib.get().asFile
        val args = buildList {
            if (withOptimizations.get()) {
                add("-opt")
                add("-Xbinary=enableReleaseBinaryCache=true")
            } else {
                add("-g")
            }
            add("-target")
            add(target.get())
            add("-produce")
            add("static_cache")
            add("-Xadd-cache=${klibFile.absolutePath}")
            add("-Xcache-directory=${cacheDirectory.get().asFile.absolutePath}")
            PlatformManager(compilerDistribution.get().root.asFile.absolutePath).apply {
                addAll(platform(targetByName(target.get())).additionalCacheFlags)
            }
            add("-Xdebug-prefix-map=${cacheDirectory.get().asFile.absolutePath}=out")
            if (makePerFileCache.get()) {
                add("-Xmake-per-file-cache")
            }
        }
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanCacheAction::class.java) {
            this.isolatedClassLoaderService.set(this@KonanCacheTask.isolatedClassLoadersService)
            this.compilerClasspath.from(this@KonanCacheTask.compilerClasspath)
            this.args.addAll(args)
        }
        workQueue.await()
        check(outDir.isDirectory && outDir.list().isNotEmpty()) {
            val cacheKind = if (makePerFileCache.get()) "Per-file cache" else "Monolithic cache"
            "$cacheKind for $klibFile for ${target.get()} failed to produce any output in $outDir"
        }
    }
}
