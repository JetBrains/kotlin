/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.facade

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

class SourceFile(
    val name: String,
    val source: String,
    val path: String = "",
) {
    fun toKtSourceFile(): KtSourceFile {
        val shortName = name.substring(name.lastIndexOf('/') + 1).let {
            it.substring(it.lastIndexOf('\\') + 1)
        }
        // LightTree parses a file as a script unless its name has the `.kt` extension
        require(shortName.endsWith(".kt")) { "Expected a .kt source file name, got '$name'" }
        return KtInMemoryTextSourceFile(
            shortName,
            "${path}/$name",
            StringUtilRt.convertLineSeparators(source)
        )
    }
}

abstract class KotlinCompilerFacade(val environment: KotlinCoreEnvironment) {
    abstract fun analyze(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>,
    ): FirAnalysisResult

    abstract fun compileToIr(files: List<SourceFile>): IrModuleFragment
    abstract fun compile(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>,
    ): GenerationState

    companion object {
        const val TEST_MODULE_NAME = "test-module"

        fun create(
            disposable: Disposable,
            updateConfiguration: CompilerConfiguration.() -> Unit,
            registerExtensions: Project.(CompilerConfiguration) -> Unit,
        ): KotlinCompilerFacade {
            val configuration = CompilerConfiguration.create().apply {
                put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE_NAME)
                put(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.ERROR)
                this.targetPlatform = JvmPlatforms.unspecifiedJvmPlatform
                put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_11)
                @OptIn(MessageCollectorAccess::class) // write access
                messageCollector = TestMessageCollector
                updateConfiguration()
                put(CommonConfigurationKeys.USE_FIR, languageVersionSettings.languageVersion.usesK2)
            }

            @OptIn(CoreEnvironmentDeprecation::class)
            val environment = KotlinCoreEnvironment.create(disposable, configuration)

            ComposePluginRegistrar.checkCompilerConfiguration(configuration)

            environment.project.registerExtensions(configuration)

            return if (configuration.languageVersionSettings.languageVersion.usesK2) {
                K2CompilerFacade(environment)
            } else {
                error("K1 is no longer supported")
            }
        }
    }
}

private object TestMessageCollector : MessageCollector {
    override fun clear() {}

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        if (severity === CompilerMessageSeverity.ERROR) {
            throw AssertionError(
                if (location == null)
                    message
                else
                    "(${location.path}:${location.line}:${location.column}) $message"
            )
        }
    }

    override fun hasErrors(): Boolean {
        return false
    }
}
