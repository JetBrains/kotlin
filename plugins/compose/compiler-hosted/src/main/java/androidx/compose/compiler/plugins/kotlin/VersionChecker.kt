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
         * A table of version ints to version strings. This should be updated every time
         * ComposeVersion.kt is updated.
         */
        private val versionTable = mapOf(
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
            3100 to "1.0.0-beta10",
        )

        /**
         * The minimum version int that this compiler is guaranteed to be compatible with. Typically
         * this will match the version int that is in ComposeVersion.kt in the runtime.
         */
        private val minimumRuntimeVersionInt: Int = 3100

        /**
         * The maven version string of this compiler. This string should be updated before/after every
         * release.
         */
        val compilerVersion: String = "1.0.0-beta10"
        private val minimumRuntimeVersion: String
            get() = versionTable[minimumRuntimeVersionInt] ?: "unknown"
    }

    fun check() {
        // version checker accesses bodies of the functions that are not deserialized in KLIB
        if (!context.platform.isJvm()) return

        val versionClass = context.referenceClass(ComposeFqNames.ComposeVersion)
        if (versionClass == null) {
            // If the version class isn't present, it likely means that compose runtime isn't on the
            // classpath anywhere. But also for dev03-dev15 there wasn't any ComposeVersion class at
            // all, so we check for the presence of the Composer class here to try and check for the
            // case that an older version of Compose runtime is available.
            val composerClass = context.referenceClass(ComposeFqNames.Composer)
            if (composerClass == null) {
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
            outdatedRuntime(versionTable[versionInt] ?: "<unknown>")
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
