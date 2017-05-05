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

package org.jetbrains.kotlin.gradle.plugin

import org.apache.commons.lang.SystemUtils
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.ZipHandler
import org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.compilerRunner.DELETED_SESSION_FILE_PREFIX
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.incremental.BuildCacheStorage
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import org.jetbrains.kotlin.incremental.relativeToRoot
import java.io.File

internal class KotlinGradleBuildServices private constructor(gradle: Gradle): BuildAdapter() {
    companion object {
        private val CLASS_NAME = KotlinGradleBuildServices::class.java.simpleName
        const val FORCE_SYSTEM_GC_MESSAGE = "Forcing System.gc()"
        val INIT_MESSAGE = "Initialized $CLASS_NAME"
        val DISPOSE_MESSAGE = "Disposed $CLASS_NAME"
        val ALREADY_INITIALIZED_MESSAGE = "$CLASS_NAME is already initialized"
        @field:Volatile
        private var instance: KotlinGradleBuildServices? = null

        @JvmStatic
        @Synchronized
        fun getInstance(gradle: Gradle): KotlinGradleBuildServices {
            val log = Logging.getLogger(KotlinGradleBuildServices::class.java)

            if (instance != null) {
                log.kotlinDebug(ALREADY_INITIALIZED_MESSAGE)
                return instance!!
            }

            val services = KotlinGradleBuildServices(gradle)
            gradle.addBuildListener(services)
            instance = services
            log.kotlinDebug(INIT_MESSAGE)

            services.buildStarted()
            return services
        }
    }

    private val log = Logging.getLogger(this.javaClass)
    private val cleanup = CompilerServicesCleanup()
    private var startMemory: Long? = null
    private val workingDir = File(gradle.rootProject.buildDir, "kotlin-build").apply { mkdirs() }
    private val buildCacheStorage = BuildCacheStorage(workingDir)

    internal val artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider
            get() = buildCacheStorage

    // There is function with the same name in BuildAdapter,
    // but it is called before any plugin can attach build listener
    fun buildStarted() {
        if (log.isDebugEnabled) {
            startMemory = getUsedMemoryKb()!!
        }
    }

    override fun buildFinished(result: BuildResult) {
        val gradle = result.gradle!!
        val kotlinCompilerCalled = gradle.rootProject
                .allprojects
                .flatMap { it.tasks }
                .any { it is AbstractKotlinCompile<*> && it.compilerCalled }

        if (kotlinCompilerCalled) {
            log.kotlinDebug("Cleanup after kotlin")
            cleanup(gradle.gradleVersion)
        }
        else {
            log.kotlinDebug("Skipping kotlin cleanup since compiler wasn't called")
        }

        val rootProject = gradle.rootProject
        val sessionsDir = GradleCompilerRunner.sessionsDir(rootProject)
        if (sessionsDir.exists()) {
            val sessionFiles = sessionsDir.listFiles()

            // it is expected that only one session file per build exists
            // afaik is is not possible to run multiple gradle builds in one project since gradle locks some dirs
            if (sessionFiles.size > 1) {
                log.warn("w: Detected multiple Kotlin daemon sessions at ${sessionsDir.relativeToRoot(rootProject)}")
            }
            for (file in sessionFiles) {
                file.delete()
                log.kotlinDebug { DELETED_SESSION_FILE_PREFIX + file.relativeToRoot(rootProject) }
            }
        }

        if (kotlinCompilerCalled) {
            startMemory?.let { startMemoryCopy ->
                getUsedMemoryKb()?.let { endMemory ->
                    // the value reported here is not necessarily a leak, since it is calculated before collecting the plugin classes
                    // but on subsequent runs in the daemon it should be rather small, then the classes are actually reused by the daemon (see above)
                    log.kotlinDebug("[PERF] Used memory after build: $endMemory kb (difference since build start: ${"%+d".format(endMemory - startMemoryCopy)} kb)")
                }
            }
        }


        if (workingDir.exists()) {
            // The working directory may have been removed by the clean task.
            // https://youtrack.jetbrains.com/issue/KT-16298
            buildCacheStorage.flush(memoryCachesOnly = false)
        }
        buildCacheStorage.close()

        gradle.removeListener(this)
        instance = null
        log.kotlinDebug(DISPOSE_MESSAGE)
    }

    private fun getUsedMemoryKb(): Long? {
        if (!log.isDebugEnabled) return null

        log.lifecycle(FORCE_SYSTEM_GC_MESSAGE)
        System.gc()
        System.runFinalization()
        System.gc()
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / 1024
    }
}


internal class CompilerServicesCleanup() {
    private val log = Logging.getLogger(this.javaClass)

    operator fun invoke(gradleVersion: String) {
        log.kotlinDebug("compiler services cleanup")

        // clearing jar cache to avoid problems like KT-9440 (unable to clean/rebuild a project due to locked jar file)
        // problem is known to happen only on windows - the reason (seems) related to http://bugs.java.com/view_bug.do?bug_id=6357433
        // clean cache only when running on windows
        if (SystemUtils.IS_OS_WINDOWS) {
            cleanJarCache()
        }

        (KotlinCoreEnvironment.applicationEnvironment?.jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
    }

    private fun cleanJarCache() {
        log.kotlinDebug("Clean JAR cache")
        ZipHandler.clearFileAccessorCache()
        log.kotlinDebug("JAR cache cleared")
    }
}
