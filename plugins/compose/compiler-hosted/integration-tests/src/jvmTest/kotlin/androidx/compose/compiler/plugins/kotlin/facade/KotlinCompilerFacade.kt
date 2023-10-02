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
import androidx.compose.compiler.plugins.kotlin.TestsCompilerError
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import java.nio.charset.StandardCharsets
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.IrMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils

class SourceFile(
    val name: String,
    val source: String,
    private val ignoreParseErrors: Boolean = false
) {
    fun toKtFile(project: Project): KtFile {
        val shortName = name.substring(name.lastIndexOf('/') + 1).let {
            it.substring(it.lastIndexOf('\\') + 1)
        }

        val virtualFile = object : LightVirtualFile(
            shortName,
            KotlinLanguage.INSTANCE,
            StringUtilRt.convertLineSeparators(source)
        ) {
            override fun getPath(): String = "/$name"
        }

        virtualFile.charset = StandardCharsets.UTF_8
        val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        val ktFile = factory.trySetupPsiForFile(
            virtualFile, KotlinLanguage.INSTANCE, true, false
        ) as KtFile

        if (!ignoreParseErrors) {
            try {
                AnalyzingUtils.checkForSyntacticErrors(ktFile)
            } catch (e: Exception) {
                throw TestsCompilerError(e)
            }
        }
        return ktFile
    }
}

interface AnalysisResult {
    data class Diagnostic(
        val factoryName: String,
        val textRanges: List<TextRange>
    )

    val files: List<KtFile>
    val diagnostics: Map<String, List<Diagnostic>>
}

abstract class KotlinCompilerFacade(val environment: KotlinCoreEnvironment) {
    abstract fun analyze(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): AnalysisResult
    abstract fun compileToIr(files: List<SourceFile>): IrModuleFragment
    abstract fun compile(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): GenerationState

    companion object {
        const val TEST_MODULE_NAME = "test-module"

        fun create(
            disposable: Disposable,
            updateConfiguration: CompilerConfiguration.() -> Unit,
            registerExtensions: Project.(CompilerConfiguration) -> Unit,
        ): KotlinCompilerFacade {
            val configuration = CompilerConfiguration().apply {
                put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE_NAME)
                put(JVMConfigurationKeys.IR, true)
                put(JVMConfigurationKeys.VALIDATE_IR, true)
                put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, TestMessageCollector)
                put(IrMessageLogger.IR_MESSAGE_LOGGER, IrMessageCollector(TestMessageCollector))
                updateConfiguration()
            }

            val environment = KotlinCoreEnvironment.createForTests(
                disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            ComposePluginRegistrar.checkCompilerVersion(configuration)

            environment.project.registerExtensions(configuration)

            return if (configuration.languageVersionSettings.languageVersion.usesK2) {
                K2CompilerFacade(environment)
            } else {
                K1CompilerFacade(environment)
            }
        }
    }
}

private object TestMessageCollector : MessageCollector {
    override fun clear() {}

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
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
