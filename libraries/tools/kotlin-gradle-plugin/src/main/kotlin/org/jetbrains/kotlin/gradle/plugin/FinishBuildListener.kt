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

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import java.lang.ref.Reference
import java.util.concurrent.ScheduledExecutorService
import kotlin.text.MatchGroup

internal fun getUsedMemoryKb(): Long {
    System.gc()
    val rt = Runtime.getRuntime()
    return (rt.totalMemory() - rt.freeMemory()) / 1024
}


private fun comparableVersionStr(version: String) =
        "(\\d+)\\.(\\d+).*"
                .toRegex()
                .match(version)
                ?.groups
                ?.drop(1)?.take(2)
                // checking if two subexpression groups are found and length of each is >0 and <4
                ?.let { if (it.all { (it?.value?.length() ?: 0).let { it > 0 && it < 4 }}) it else null }
                ?.joinToString(".", transform = { it!!.value.padStart(3, '0') })


class FinishBuildListener(pluginClassLoader: ClassLoader?, val startMemory: Long) : BuildAdapter() {
    val log = Logging.getLogger(this.javaClass)

    private var threadTracker: ThreadTracker? = ThreadTracker()

    private val cleanup = CompilerServicesCleanup(pluginClassLoader)

    override fun buildFinished(result: BuildResult?) {
        log.kotlinDebug("Build finished listener")

        val gradle = result?.getGradle()
        if (gradle != null) {
            // making cleanup only on recognized versions of gradle and if version < 2.4
            // otherwise it may cause problems e.g. with JobScheduler on subsequent runs
            // this strategy may lead to memory leaks, but prevent crashes due to destroyed JobScheduler
            // the reason for the strategy is the following:
            // gradle < 2.4 has problems with plugin reuse in the daemon: new calls to the plugin are made with a new classloader
            // for every new build. With statically initialized daemons like JobScheduler that leads to big leaks of classloaders and classes,
            // therefore to reduce leaks JobScheduler (and deprecated ZipFileCache for now) should be stopped)
            // It should be noted that because of this behavior there are no benefits of using daemon in these versions.
            // Starting from 2.4 gradle using cached classloaders, that leads to effective class reusing in the daemon, but
            // in that case premature stopping of the static daemons may lead to crashes.
            comparableVersionStr(gradle.getGradleVersion())?.let {
                log.kotlinDebug("detected gradle version $it")
                if (it < comparableVersionStr("2.4")!!) {
                    cleanup()
                    // checking thread leaks only then cleaning up
                    threadTracker?.checkThreadLeak(gradle)
                }
            }

            threadTracker = null
            gradle.removeListener(this)
        }

        // the value reported here is not necessarily a leak, since it is calculated before collecting the plugin classes
        // but on subsequent runs in the daemon it should be rather small, then the classes are actually reused by the daemon (see above)
        getUsedMemoryKb().let { log.kotlinDebug("[PERF] Used memory after build: $it kb (${"%+d".format(it - startMemory)} kb)") }
    }
}


class CompilerServicesCleanup(private var pluginClassLoader: ClassLoader?) {
    val log = Logging.getLogger(this.javaClass)

    fun invoke() {
        assert(pluginClassLoader != null)

        log.kotlinDebug("compiler services cleanup")

        // TODO: remove ZipFileCache cleanup after switching to recent idea libs
        stopZipFileCache()
        stopJobScheduler()

        pluginClassLoader = null
    }

    private fun stopZipFileCache() {
        callVoidStaticMethod("com.intellij.openapi.util.io.ZipFileCache", "stopBackgroundThread")
        log.kotlinDebug("ZipFileCache finished successfully")
    }

    private fun stopJobScheduler() {
        log.kotlinDebug("Stop JobScheduler")

        val jobSchedulerClass = Class.forName("com.intellij.concurrency.JobScheduler", false, pluginClassLoader)

        val getSchedulerMethod = jobSchedulerClass.getMethod("getScheduler")
        val executorService = getSchedulerMethod.invoke(this) as ScheduledExecutorService

        executorService.shutdown()
        log.kotlinDebug("JobScheduler stopped")
    }

    private fun callVoidStaticMethod(classFqName: String, methodName: String) {
        val shortName = classFqName.substring(classFqName.lastIndexOf('.') + 1)

        log.kotlinDebug("Looking for $shortName class")
        val lowMemoryWatcherClass = Class.forName(classFqName, false, pluginClassLoader)

        log.kotlinDebug("Looking for $methodName() method")
        val shutdownMethod = lowMemoryWatcherClass.getMethod(methodName)

        log.kotlinDebug("Call $shortName.$methodName()")
        shutdownMethod.invoke(null)
    }
}
