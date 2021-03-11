/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.VersionView
import org.jetbrains.kotlin.jps.incremental.CacheStatus
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.getKotlinCache
import org.jetbrains.kotlin.jps.model.kotlinCompilerArguments
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import java.io.File

/**
 * Chunk of cyclically dependent [KotlinModuleBuildTarget]s
 */
class KotlinChunk internal constructor(val context: KotlinCompileContext, val targets: List<KotlinModuleBuildTarget<*>>) {
    val containsTests = targets.any { it.isTests }

    lateinit var dependencies: List<KotlinModuleBuildTarget.Dependency>
        // Should be initialized only in KotlinChunk.calculateChunkDependencies
        internal set

    lateinit var dependent: List<KotlinModuleBuildTarget.Dependency>
        // Should be initialized only in KotlinChunk.calculateChunkDependencies
        internal set

    // used only during dependency calculation
    internal var _dependent: MutableSet<KotlinModuleBuildTarget.Dependency>? = mutableSetOf()

    val representativeTarget
        get() = targets.first()

    val presentableModulesToCompilersList: String
        get() = targets.joinToString { "${it.module.name} (${it.globalLookupCacheId})" }

    val haveSameCompiler = targets.all { it.javaClass == representativeTarget.javaClass }

    private val defaultLanguageVersion = VersionView.RELEASED_VERSION

    val compilerArguments = representativeTarget.jpsModuleBuildTarget.module.kotlinCompilerArguments.also {
        it.reportOutputFiles = true

        // Always report the version to help diagnosing user issues if they submit the compiler output
        it.version = true

        if (it.languageVersion == null) it.languageVersion = defaultLanguageVersion.versionString
    }

    val langVersion =
        compilerArguments.languageVersion?.let { LanguageVersion.fromVersionString(it) }
            ?: defaultLanguageVersion // use default language version when version string is invalid (todo: report warning?)

    val apiVersion =
        compilerArguments.apiVersion?.let { ApiVersion.parse(it) }
            ?: ApiVersion.createByLanguageVersion(langVersion) // todo: report version parse error?

    val isEnabled: Boolean = representativeTarget.isEnabled(compilerArguments)

    fun shouldRebuild(): Boolean {
        val buildMetaInfo = representativeTarget.buildMetaInfoFactory.create(compilerArguments)

        targets.forEach { target ->
            if (target.isVersionChanged(this, buildMetaInfo)) {
                KotlinBuilder.LOG.info("$target version changed, rebuilding $this")
                return true
            }

            if (target.initialLocalCacheAttributesDiff.status == CacheStatus.INVALID) {
                context.testingLogger?.invalidOrUnusedCache(this, null, target.initialLocalCacheAttributesDiff)
                KotlinBuilder.LOG.info("$target cache is invalid ${target.initialLocalCacheAttributesDiff}, rebuilding $this")
                return true
            }
        }

        return false
    }

    fun buildMetaInfoFile(target: ModuleBuildTarget): File =
        File(
            context.dataPaths.getTargetDataRoot(target),
            representativeTarget.buildMetaInfoFileName
        )

    fun saveVersions() {
        context.ensureLookupsCacheAttributesSaved()

        targets.forEach {
            it.initialLocalCacheAttributesDiff.manager.writeVersion()
        }

        val serializedMetaInfo = representativeTarget.buildMetaInfoFactory.serializeToString(compilerArguments)

        targets.forEach {
            buildMetaInfoFile(it.jpsModuleBuildTarget).writeText(serializedMetaInfo)
        }
    }

    fun collectDependentChunksRecursivelyExportedOnly(result: MutableSet<KotlinChunk> = mutableSetOf()) {
        dependent.forEach {
            if (result.add(it.src.chunk)) {
                if (it.exported) {
                    it.src.chunk.collectDependentChunksRecursivelyExportedOnly(result)
                }
            }
        }
    }

    fun loadCaches(loadDependent: Boolean = true): Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache> {
        val dataManager = context.dataManager

        val cacheByChunkTarget = targets.keysToMapExceptNulls {
            dataManager.getKotlinCache(it)
        }

        if (loadDependent) {
            addDependentCaches(cacheByChunkTarget.values)
        }

        return cacheByChunkTarget
    }

    private fun addDependentCaches(targetsCaches: Collection<JpsIncrementalCache>) {
        val dependentChunks = mutableSetOf<KotlinChunk>()

        collectDependentChunksRecursivelyExportedOnly(dependentChunks)

        val dataManager = context.dataManager
        dependentChunks.forEach { decedentChunk ->
            decedentChunk.targets.forEach {
                val dependentCache = dataManager.getKotlinCache(it)
                if (dependentCache != null) {

                    for (chunkCache in targetsCaches) {
                        chunkCache.addJpsDependentCache(dependentCache)
                    }
                }
            }
        }
    }

    /**
     * The same as [org.jetbrains.jps.ModuleChunk.getPresentableShortName]
     */
    val presentableShortName: String
        get() = buildString {
            if (containsTests) append("tests of ")
            append(targets.first().module.name)
            if (targets.size > 1) {
                val andXMore = " and ${targets.size - 1} more"
                val other = ", " + targets.asSequence().drop(1).joinToString()
                append(if (other.length < andXMore.length) other else andXMore)
            }
        }

    override fun toString(): String {
        return "KotlinChunk<${representativeTarget.javaClass.simpleName}>" +
                "(${targets.joinToString { it.jpsModuleBuildTarget.presentableName }})"
    }
}