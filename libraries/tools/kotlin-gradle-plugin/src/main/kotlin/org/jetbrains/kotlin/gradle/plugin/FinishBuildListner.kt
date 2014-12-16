/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.gradle.api.logging.Logging
import org.gradle.BuildResult
import java.lang.ref.Reference
import java.lang.reflect.Array
import kotlin.platform.platformStatic

class FinishBuildListener(var pluginClassLoader: ParentLastURLClassLoader?) : BuildAdapter() {
    val log = Logging.getLogger(this.javaClass)

    class object {
        platformStatic
        fun isRequestedClass(fqName: String): Boolean {
            return when (fqName) {
                "com.intellij.openapi.util.io.ZipFileCache",
                "com.intellij.openapi.util.LowMemoryWatcher" -> true
                else -> false
            }
        }
    }

    override fun buildFinished(result: BuildResult?) {
        log.debug("Build finished listener")

        stopZipFileCache()
        stopLowMemoryWatcher()

		// TODO: Try to clean up thread locals without this ugly hack
		// TODO: Further investigation of PermGen leak (KT-6451)
        removeThreadLocals()

        pluginClassLoader = null
        result?.getGradle()?.removeListener(this)
    }

    public fun removeThreadLocals() {
        try {
            log.debug("Remove ChildURLClassLoader thread locals")

            val thread = Thread.currentThread()
            val threadLocalsField = javaClass<Thread>().getDeclaredField("threadLocals")
            threadLocalsField.setAccessible(true)

            val threadLocalMapClass = Class.forName("java.lang.ThreadLocal\$ThreadLocalMap")
            val tableField = threadLocalMapClass.getDeclaredField("table")
            tableField.setAccessible(true)

            val referentField = javaClass<Reference<Any>>().getDeclaredField("referent")
            referentField.setAccessible(true)

            val table = tableField[threadLocalsField[thread]] as kotlin.Array<*>

            for (entry in table) {
                if (entry != null) {
                    val threadLocal = referentField[entry] as ThreadLocal<*>?
                    val classLoader = threadLocal?.javaClass?.getClassLoader()
                    if (classLoader is ParentLastURLClassLoader.ChildURLClassLoader) {
                        threadLocal?.remove()
                    }
                }
            }

            log.debug("Removing ChildURLClassLoader thread locals finished successfully")
        } catch (e: Throwable) {
            log.debug("Exception during thread locals remove: " + e)
        }
    }

    private fun stopZipFileCache() {
        callVoidStaticMethod("com.intellij.openapi.util.io.ZipFileCache", "shutdown")
        log.debug("ZipFileCache finished successfully")
    }

    private fun stopLowMemoryWatcher() {
        callVoidStaticMethod("com.intellij.openapi.util.LowMemoryWatcher", "shutdown")
        log.debug("LowMemoryWatcher finished successfully")
    }

    private fun callVoidStaticMethod(classFqName: String, methodName: String) {
        val shortName = classFqName.substring(classFqName.lastIndexOf('.') + 1)

        log.debug("Looking for $shortName class")
        val lowMemoryWatcherClass = Class.forName(classFqName, false, pluginClassLoader)

        log.debug("Looking for $methodName() method")
        val shutdownMethod = lowMemoryWatcherClass.getMethod(methodName)

        log.debug("Call $shortName.$methodName()")
        shutdownMethod.invoke(null)
    }
}
