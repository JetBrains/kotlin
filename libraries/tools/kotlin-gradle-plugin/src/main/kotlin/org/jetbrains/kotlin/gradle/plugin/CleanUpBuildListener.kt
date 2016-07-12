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
import org.gradle.api.Project
import org.gradle.api.logging.Logging


private fun comparableVersionStr(version: String) =
        "(\\d+)\\.(\\d+).*"
                .toRegex()
                .find(version)
                ?.groups
                ?.drop(1)?.take(2)
                // checking if two subexpression groups are found and length of each is >0 and <4
                ?.let { if (it.all { (it?.value?.length ?: 0).let { it > 0 && it < 4 }}) it else null }
                ?.joinToString(".", transform = { it!!.value.padStart(3, '0') })

class CleanUpBuildListener(pluginClassLoader: ClassLoader, private val project: Project) : BuildAdapter() {
    companion object {
        const val FORCE_SYSTEM_GC_MESSAGE = "Forcing System.gc()"
    }

    private val log = Logging.getLogger(this.javaClass)
    private val cleanup = CompilerServicesCleanup(pluginClassLoader)
    private var startMemory: Long? = null

    // There is function with the same name in BuildAdapter,
    // but it is called before any plugin can attach build listener
    fun buildStarted() {
        if (log.isDebugEnabled) {
            startMemory = getUsedMemoryKb()!!
        }
    }

    override fun buildFinished(result: BuildResult?) {
        val gradle = result?.gradle
        if (gradle != null) {

            val kotlinCompilerCalled = project.tasks.filter { it.name.contains("kotlin", ignoreCase = true) }
                    .any { task -> task.hasProperty("compilerCalled") && task.property("compilerCalled") as? Boolean ?: false }

            if (kotlinCompilerCalled) {
                log.kotlinDebug("Cleanup after kotlin")

                cleanup(gradle.gradleVersion)
            }
            else {
                log.kotlinDebug("Skipping kotlin cleanup since compiler wasn't called")
            }

            gradle.removeListener(this)

            if (kotlinCompilerCalled) {
                startMemory?.let { startMemoryCopy ->
                    getUsedMemoryKb()?.let { endMemory ->
                        // the value reported here is not necessarily a leak, since it is calculated before collecting the plugin classes
                        // but on subsequent runs in the daemon it should be rather small, then the classes are actually reused by the daemon (see above)
                        log.kotlinDebug("[PERF] Used memory after build: $endMemory kb (difference since build start: ${"%+d".format(endMemory - startMemoryCopy)} kb)")
                    }
                }
            }
        }
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


class CompilerServicesCleanup(private var pluginClassLoader: ClassLoader?) {
    val log = Logging.getLogger(this.javaClass)

    operator fun invoke(gradleVersion: String) {
        assert(pluginClassLoader != null)

        log.kotlinDebug("compiler services cleanup")

        // clearing jar cache to avoid problems like KT-9440 (unable to clean/rebuild a project due to locked jar file)
        // problem is known to happen only on windows - the reason (seems) related to http://bugs.java.com/view_bug.do?bug_id=6357433
        // clean cache only when running on windows
        if (SystemUtils.IS_OS_WINDOWS) {
            cleanJarCache()
        }

        // making cleanup of static objects only on recognized versions of gradle and if version < 2.4
        // otherwise it may cause problems e.g. with JobScheduler on subsequent runs
        // this strategy may lead to memory leaks, but prevent crashes due to destroyed JobScheduler
        // the reason for the strategy is the following:
        // gradle < 2.4 has problems with plugin reuse in the daemon: new calls to the plugin are made with a new classloader
        // for every new build. With statically initialized daemons like JobScheduler that leads to big leaks of classloaders and classes,
        // therefore to reduce leaks JobScheduler (and deprecated ZipFileCache for now) should be stopped)
        // It should be noted that because of this behavior there are no benefits of using daemon in these versions.
        // Starting from 2.4 gradle using cached classloaders, that leads to effective class reusing in the daemon, but
        // in that case premature stopping of the static daemons may lead to crashes.
        comparableVersionStr(gradleVersion)?.let {
            log.kotlinDebug("detected gradle version $it")
            if (it < comparableVersionStr("2.4")!!) {
                // TODO: remove ZipFileCache cleanup after switching to recent idea libs
                stopZipFileCache()
            }
        }

        pluginClassLoader = null
    }

    private fun stopZipFileCache() {
        callVoidStaticMethod("com.intellij.openapi.util.io.ZipFileCache", "stopBackgroundThread")
        log.kotlinDebug("ZipFileCache finished successfully")
    }

    private fun callVoidStaticMethod(classFqName: String, methodName: String) {
        val shortName = classFqName.substring(classFqName.lastIndexOf('.') + 1)

        log.kotlinDebug("Looking for $shortName class")
        val cls = pluginClassLoader!!.loadClass(classFqName)

        log.kotlinDebug("Looking for $methodName() method")
        val method = cls.getMethod(methodName)

        log.kotlinDebug("Call $shortName.$methodName()")
        method.invoke(null)
    }

    private fun cleanJarCache() {
        log.kotlinDebug("Clean JAR cache")
        callVoidStaticMethod("com.intellij.openapi.vfs.impl.ZipHandler", "clearFileAccessorCache")
        log.kotlinDebug("JAR cache cleared")
    }
}
