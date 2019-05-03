/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.kotlin

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace

object JvmResolveUtil {
    @JvmStatic
    fun analyzeAndCheckForErrors(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace()
    ): AnalysisResult {
        for (file in files) {
            try {
                AnalyzingUtils.checkForSyntacticErrors(file)
            } catch (e: Exception) {
                throw TestsCompilerError(e)
            }
        }

        return analyze(
            project,
            files,
            configuration,
            packagePartProvider,
            trace
        ).apply {
            try {
                AnalyzingUtils.throwExceptionOnErrors(bindingContext)
            } catch (e: Exception) {
                throw TestsCompilerError(e)
            }
        }
    }

    @JvmStatic
    fun analyze(file: KtFile, environment: KotlinCoreEnvironment): AnalysisResult =
        analyze(setOf(file), environment)

    @JvmStatic
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult =
        analyze(
            files,
            environment,
            environment.configuration
        )

    @JvmStatic
    fun analyze(
        files: Collection<KtFile>,
        environment: KotlinCoreEnvironment,
        configuration: CompilerConfiguration
    ): AnalysisResult =
        analyze(
            environment.project,
            files,
            configuration,
            environment::createPackagePartProvider
        )

    private fun analyze(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProviderFactory: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace()
    ): AnalysisResult {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project, files, trace, configuration, packagePartProviderFactory
        )
    }
}
