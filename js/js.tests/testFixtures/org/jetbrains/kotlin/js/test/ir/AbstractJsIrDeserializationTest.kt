/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.IGNORE_IR_DESERIALIZATION_TEST
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Base class for IR deserialization tests, configured with FIR frontend.
 */
abstract class AbstractJsIrDeserializationTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    private val useIrInlinerAtFirstCompilationPhase: Boolean
) : AbstractJsBlackBoxCodegenTestBase<FirOutputArtifact>(FrontendKinds.FIR, TargetBackend.JS_IR, pathToTestDir, testGroupOutputDirPrefix) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliWebFacade

    override val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliWebFacade

    override val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirKlibSerializerCliWebFacade

    override val backendFacades: JsBackendFacades
        get() = JsBackendFacades.WithSeparatedDeserialization

    override val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = IGNORE_IR_DESERIALIZATION_TEST

    override val enableBoxHandlers: Boolean
        get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                runIf(useIrInlinerAtFirstCompilationPhase) {
                    LANGUAGE with listOf(
                        "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                        "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                    )
                }
                +JsEnvironmentConfigurationDirectives.PER_MODULE
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
            }
            useAfterAnalysisCheckers(
                ::FirMetaInfoDiffSuppressor
            )
        }
    }
}

open class AbstractJsIrDeserializationCodegenBoxTest : AbstractJsIrDeserializationTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "irDeserialization/codegenBox/",
    useIrInlinerAtFirstCompilationPhase = false,
)

open class AbstractJsIrDeserializationCodegenBoxWithInlinedFunInKlibTest : AbstractJsIrDeserializationTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "irDeserialization/codegenBoxWithInlinedFunInKlib/",
    useIrInlinerAtFirstCompilationPhase = true,
)
