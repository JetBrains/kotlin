/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend

import com.intellij.openapi.project.Project
import org.jebrains.kotlin.backend.native.BaseNativeConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseContext
import org.jetbrains.kotlin.backend.common.phaser.startTopLevel
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.driver.FirSerializerInput
import org.jetbrains.kotlin.backend.driver.runFir2Ir
import org.jetbrains.kotlin.backend.driver.runFir2IrSerializer
import org.jetbrains.kotlin.backend.driver.runFirFrontend
import org.jetbrains.kotlin.backend.driver.runFirSerializer
import org.jetbrains.kotlin.backend.driver.runPreSerializationLowerings
import org.jetbrains.kotlin.backend.driver.writeKlib
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime

class NativeFrontendDriver(private val performanceManager: PerformanceManager?) {

    fun run(config: BaseNativeConfig, environment: KotlinCoreEnvironment, project: Project) {
        PhaseEngine.startTopLevel(config.configuration) { engine ->
            produceKlib(engine, config, environment, project)
        }
    }

    private fun produceKlib(
        engine: PhaseEngine<PhaseContext>,
        config: BaseNativeConfig,
        environment: KotlinCoreEnvironment,
        project: Project,
    ) {
        engine.useContext(FrontendContextImpl(NativeFrontendConfig(config))) { engine ->
            val serializerOutput = serializeKLibK2(engine, engine.context.config, environment, project)
            serializerOutput?.let {
                performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                    engine.writeKlib(it)
                }
            }
        }
    }

    private fun serializeKLibK2(
        engine: PhaseEngine<out FrontendContext>,
        config: NativeFrontendConfig,
        environment: KotlinCoreEnvironment,
        project: Project,
    ): SerializerOutput? {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFirFrontend(environment) }
        if (frontendOutput is FirOutput.ShouldNotGenerateCode) return null
        require(frontendOutput is FirOutput.Full)

        return if (config.metadataKlib) {
            performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runFirSerializer(frontendOutput)
            }
        } else {
            val fir2IrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
                engine.runFir2Ir(Fir2IrInput(frontendOutput, project, config.resolvedLibraries))
            }
            val headerKlibPath = config.headerKlibPath
            if (!headerKlibPath.isNullOrEmpty()) {
                val headerKlib = performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                    engine.runFir2IrSerializer(FirSerializerInput(fir2IrOutput, produceHeaderKlib = true))
                }
                performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                    engine.writeKlib(headerKlib, headerKlibPath, produceHeaderKlib = true)
                }
                // Don't overwrite the header klib with the full klib and stop compilation here.
                // By providing the same path for both regular output and header klib we can skip emitting the full klib.
                if (File(config.outputPath).canonicalPath == File(headerKlibPath).canonicalPath) return null
            }

            val loweredIr = performanceManager.tryMeasurePhaseTime(PhaseType.IrPreLowering) {
                engine.runPreSerializationLowerings(fir2IrOutput, environment)
            }
            performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runFir2IrSerializer(FirSerializerInput(loweredIr))
            }
        }
    }
}