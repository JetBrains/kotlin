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

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils

object GenerationUtils {
    fun compileFiles(
        environment: KotlinCoreEnvironment,
        files: List<KtFile>,
    ): GenerationState {
        val analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(environment, files)
        analysisResult.throwIfError()

        val state = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.TEST,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            files,
            environment.configuration
        ).codegenFactory(
            JvmIrCodegenFactory(
                environment.configuration,
                environment.configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
                    ?: PhaseConfig(jvmPhases)
            )
        ).isIrBackend(true).build()

        KotlinCodegenFacade.compileCorrectFiles(state)

        // For JVM-specific errors
        try {
            AnalyzingUtils.throwExceptionOnErrors(state.collectedExtraJvmDiagnostics)
        } catch (e: Throwable) {
            throw TestsCompilerError(e)
        }

        return state
    }
}
