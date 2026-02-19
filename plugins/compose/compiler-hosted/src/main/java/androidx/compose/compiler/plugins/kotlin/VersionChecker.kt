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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.platform.jvm.isJvm

enum class VersionCheckerResult {
    SUCCESS,
    NOT_FOUND,
}

class VersionChecker(val context: IrPluginContext, private val messageCollector: MessageCollector) {

    companion object {
        /**
         * A table of runtime version ints to version strings for compose-runtime. Versions past
         * 3300 do not need to be included in this table because `minimumRuntimeVersionInt` must
         * remain fixed at 3300.
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
        )

        /**
         * The minimum version int that this compiler is guaranteed to be compatible with. This
         * value must remain fixed at 3300.
         */
        private const val minimumRuntimeVersionInt: Int = 3300

        private const val minimumRuntimeVersion = "1.0.0"
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun check(skipIfRuntimeNotFound: Boolean = false): VersionCheckerResult {
        val versionClass = context.finderForBuiltins().findClass(ComposeClassIds.ComposeVersion)
        if (versionClass == null) {
            // If it is a Compose app, it will depend on Compose runtime. Therefore, we must be
            // able to find ComposeVersion. If it is a non-Compose app, we skip this IR lowering.
            if (skipIfRuntimeNotFound) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING, """
                        The Compose Compiler requires the Compose Runtime to be on the classpath, but
                        none could be found. Skipping transform because
                        skipIrLoweringIfRuntimeNotFound flag was passed to the compiler.
                    """.trimIndent().replace('\n', ' ')
                )
                return VersionCheckerResult.NOT_FOUND
            }

            // If the version class isn't present, it likely means that compose runtime isn't on the
            // classpath anywhere. But also for dev03-dev15 there wasn't any ComposeVersion class at
            // all, so we check for the presence of the Composer class here to try and check for the
            // case that an older version of Compose runtime is available.
            val composerClass = context.finderForBuiltins().findClass(ComposeClassIds.Composer)
            if (composerClass != null) {
                outdatedRuntimeWithUnknownVersionNumber()
            } else {
                noRuntimeOnClasspathError()
            }
        }

        // The check accesses bodies of the functions that are not deserialized in KLIB
        if (!context.platform.isJvm()) return VersionCheckerResult.SUCCESS

        val versionExpr = versionClass
            .owner
            .declarations
            .mapNotNull { it as? IrProperty }
            .firstOrNull { it.name.asString() == "version" }
            ?.backingField
            ?.initializer
            ?.expression
                as? IrConst
        if (versionExpr == null || versionExpr.kind != IrConstKind.Int) {
            outdatedRuntimeWithUnknownVersionNumber()
        }
        val versionInt = versionExpr.value as Int
        if (versionInt < minimumRuntimeVersionInt) {
            outdatedRuntime(runtimeVersionToMavenVersionTable[versionInt] ?: "<unknown>")
        }
        // success. We are compatible with this runtime version!
        return VersionCheckerResult.SUCCESS
    }

    private fun noRuntimeOnClasspathError(): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                The Compose Compiler requires the Compose Runtime to be on the class path, but
                none could be found. The compose compiler plugin you are using expects a minimum
                runtime version of $minimumRuntimeVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }

    private fun outdatedRuntimeWithUnknownVersionNumber(): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                You are using an outdated version of Compose Runtime that is not compatible with
                the version of the Compose Compiler plugin you have installed. The compose
                compiler plugin you are using expects a minimum runtime version of
                $minimumRuntimeVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }

    private fun outdatedRuntime(actualVersion: String): Nothing {
        throw IncompatibleComposeRuntimeVersionException(
            """
                You are using an outdated version of Compose Runtime that is not compatible with
                the version of the Compose Compiler plugin you have installed. The compose
                compiler plugin you are using expects a minimum runtime version of
                $minimumRuntimeVersion. The version of the runtime on the classpath currently is
                $actualVersion.
            """.trimIndent().replace('\n', ' ')
        )
    }
}

class IncompatibleComposeRuntimeVersionException(override val message: String) : Exception(message)
