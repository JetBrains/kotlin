/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.gradle.plugin.konan.registerIsolatedClassLoadersServiceIfAbsent
import org.jetbrains.kotlin.gradle.plugin.konan.runKonanTool
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty
import java.io.File
import java.util.*
import javax.inject.Inject

enum class KonanCacheKind(val outputKind: CompilerOutputKind) {
    STATIC(CompilerOutputKind.STATIC_CACHE),
    DYNAMIC(CompilerOutputKind.DYNAMIC_CACHE)
}

abstract class KonanCacheTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:InputDirectory
    abstract val originalKlib: DirectoryProperty

    @get:Input
    lateinit var klibUniqName: String

    @get:Input
    lateinit var cacheRoot: String

    @get:Input
    lateinit var target: String

    @get:Internal
    // TODO: Reuse NativeCacheKind from Big Kotlin plugin when it is available.
    val cacheDirectory: File
        get() = File("$cacheRoot/$target-g$cacheKind")

    @get:OutputDirectory
    val cacheFile: File
        get() = cacheDirectory.resolve(if (makePerFileCache) "${klibUniqName}-per-file-cache" else "${klibUniqName}-cache")

    @get:Input
    var cacheKind: KonanCacheKind = KonanCacheKind.STATIC

    @get:Input
    var makePerFileCache: Boolean = false

    @get:Internal
    val compilerDistribution: NativeDistributionProperty = objectFactory.nativeDistributionProperty()

    @get:Input
    /** Path to a compiler distribution that is used to build this cache. */
    val compilerDistributionPath: Provider<File> = compilerDistribution.map { it.root.asFile }

    @get:Input
    var cachedLibraries: Map<File, File> = emptyMap()

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    @TaskAction
    fun compile() {
        // Compiler doesn't create a cache if the cacheFile already exists. So we need to remove it manually.
        cacheFile.prepareAsOutput()

        val konanHome = compilerDistributionPath.get().absolutePath
        val additionalCacheFlags = PlatformManager(konanHome).let {
            it.targetByName(target).let(it::loader).additionalCacheFlags
        }
        require(originalKlib.isPresent)
        val args = mutableListOf(
            "-g",
            "-target", target,
            "-produce", cacheKind.outputKind.name.lowercase(Locale.getDefault()),
            "-Xadd-cache=${originalKlib.asFile.get().absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}"
        )
        if (makePerFileCache)
            args += "-Xmake-per-file-cache"
        args += additionalCacheFlags
        args += cachedLibraries.map { "-Xcached-library=${it.key},${it.value}" }

        isolatedClassLoadersService.get().getClassLoader(compilerDistribution.get().compilerClasspath.files).runKonanTool(
                toolName = "konanc",
                args = args,
                useArgFile = true,
        )
    }
}
