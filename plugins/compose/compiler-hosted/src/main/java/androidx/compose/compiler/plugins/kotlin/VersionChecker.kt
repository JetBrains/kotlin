/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.platform.jvm.isJvm

class VersionChecker(val context: IrPluginContext) {

    companion object {
        /**
         * A table of runtime version ints to version strings for compose-runtime.
         * This should be updated every time a new version of the Compose Runtime is released.
         * Typically updated via update_versions_for_release.py
         */
        private val runtimeVersionToMavenVersionTable = mapOf(
            1600 to "0.1.0-dev16",
            1700 to "1.0.0-alpha06",
            1800 to "1.0.0-alpha07",
            1900 to "1.0.0-alpha08",
            2000 to "1.0.0-alpha09",
            2100 to "1.0.0-alpha10",
            2200 to "1.0.0-alpha11",
            2300 to "1.0.0-alpha12",
            2400 to "1.0.0-alpha13",
            2500 to "1.0.0-beta04",
            2600 to "1.0.0-beta05",
            2700 to "1.0.0-beta06",
            2800 to "1.0.0-beta07",
            2900 to "1.0.0-beta08",
            3000 to "1.0.0-beta09",
            3100 to "1.0.0-rc01",
            3200 to "1.0.0-rc02",
            3300 to "1.0.0",
            3301 to "1.0.1",
            3302 to "1.0.2",
            3303 to "1.0.3",
            3304 to "1.0.4",
            3305 to "1.0.5",
            4000 to "1.1.0-alpha01",
            4100 to "1.1.0-alpha02",
            4200 to "1.1.0-alpha03",
            4300 to "1.1.0-alpha04",
            4400 to "1.1.0-alpha05",
            4500 to "1.1.0-alpha06",
            4600 to "1.1.0-beta01",
            4700 to "1.1.0-beta02",
            4800 to "1.1.0-beta03",
            4900 to "1.1.0-beta04",
            5000 to "1.1.0-rc01",
            5001 to "1.1.0-rc02",
            5002 to "1.1.0-rc03",
            5003 to "1.1.0",
            5004 to "1.1.1",
            6000 to "1.2.0-alpha01",
            6100 to "1.2.0-alpha02",
            6200 to "1.2.0-alpha03",
            6300 to "1.2.0-alpha04",
            6400 to "1.2.0-alpha05",
            6500 to "1.2.0-alpha06",
            6600 to "1.2.0-alpha07",
            6700 to "1.2.0-alpha08",
            6800 to "1.2.0-beta01",
            6900 to "1.2.0-beta02",
            7000 to "1.2.0-beta03",
            7100 to "1.2.0-rc01",
            7101 to "1.2.0-rc02",
            7102 to "1.2.0-rc03",
            7103 to "1.2.0",
            7104 to "1.2.1",
            7105 to "1.2.2",
            8000 to "1.3.0-alpha01",
            8100 to "1.3.0-alpha02",
            8200 to "1.3.0-alpha03",
            8300 to "1.3.0-beta01",
            8400 to "1.3.0-beta02",
            8500 to "1.3.0-beta03",
            8600 to "1.3.0-rc01",
            8601 to "1.3.0-rc02",
            8602 to "1.3.0",
            8603 to "1.3.1",
            8604 to "1.3.2",
            8605 to "1.3.3",
            8606 to "1.3.4",
            9000 to "1.4.0-alpha01",
            9001 to "1.4.0-alpha02",
            9100 to "1.4.0-alpha03",
            9200 to "1.4.0-alpha04",
            9300 to "1.4.0-alpha05",
            9400 to "1.4.0-alpha06",
            9500 to "1.4.0-beta01",
            9600 to "1.4.0-beta02",
            9700 to "1.4.0-rc01",
            9701 to "1.4.0-rc02",
            9701 to "1.5.0-alpha01",
            9801 to "1.5.0-alpha02",
            9901 to "1.5.0-alpha03",
            10001 to "1.5.0-alpha04",
        )

        /**
         * The minimum version int that this compiler is guaranteed to be compatible with. Typically
         * this will match the version int that is in ComposeVersion.kt in the runtime.
         */
        private const val minimumRuntimeVersionInt: Int = 3300

        /**
         * The maven version string of this compiler. This string should be updated before/after every
         * release.
         */
        const val compilerVersion: String = "1.4.7"
        private val minimumRuntimeVersion: String
            get() = runtimeVersionToMavenVersionTable[minimumRuntimeVersionInt] ?: "unknown"
    }

    fun check() {
        // version checker accesses bodies of the functions that are not deserialized in KLIB
        if (!context.platform.isJvm()) return

        val versionClass = context.referenceClass(ComposeClassIds.ComposeVersion)
        if (versionClass == null) {
            // If the version class isn't present, it likely means that compose runtime isn't on the
            // classpath anywhere. But also for dev03-dev15 there wasn't any ComposeVersion class at
            // all, so we check for the presence of the Composer class here to try and check for the
            // case that an older version of Compose runtime is available.
            val composerClass = context.referenceClass(ComposeClassIds.Composer)
            if (composerClass != null) {
                outdatedRuntimeWithUnknownVersionNumber()
            } else {
                noRuntimeOnClasspathError()
            }
        }
        val versionExpr = versionClass
            .owner
            .declarations
            .mapNotNull { it as? IrProperty }
            .firstOrNull { it.name.asString() == "version" }
            ?.backingField
            ?.initializer
            ?.expression
            as? IrConst<*>
        if (versionExpr == null || versionExpr.kind != IrConstKind.Int) {
            outdatedRuntimeWithUnknownVersionNumber()
        }
        val versionInt = versionExpr.value as Int
        if (versionInt < minimumRuntimeVersionInt) {
            outdatedRuntime(runtimeVersionToMavenVersionTable[versionInt] ?: "<unknown>")
        }
        // success. We are compatible with this runtime version!
    }

    private fun noRuntimeOnClasspathError(): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                The Compose Compiler requires the Compose Runtime to be on the class path, but
                none could be found. The compose compiler plugin you are using (version
                $compilerVersion) expects a minimum runtime version of $minimumRuntimeVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }

    private fun outdatedRuntimeWithUnknownVersionNumber(): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                You are using an outdated version of Compose Runtime that is not compatible with
                the version of the Compose Compiler plugin you have installed. The compose
                compiler plugin you are using (version $compilerVersion) expects a minimum runtime
                version of $minimumRuntimeVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }

    private fun outdatedRuntime(actualVersion: String): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                You are using an outdated version of Compose Runtime that is not compatible with
                the version of the Compose Compiler plugin you have installed. The compose
                compiler plugin you are using (version $compilerVersion) expects a minimum runtime
                version of $minimumRuntimeVersion. The version of the runtime on the classpath
                currently is $actualVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }
}

class IncompatibleComposeRuntimeVersionException(override val message: String) : Exception(message)
