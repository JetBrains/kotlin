/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.unit

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.dataframe.functionSymbol
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

abstract class DataFrameUnitTests(val assertion: (FirFunctionCall) -> Unit) : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        defaultDirectives {
            +ENABLE_PLUGIN_PHASES
        }

        class Subject(testServices: TestServices) : EnvironmentConfigurator(testServices) {
            override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
                FirExtensionRegistrar.registerExtension(project, object : FirExtensionRegistrar() {
                    override fun ExtensionRegistrarContext.configurePlugin() {
                        +{ it: FirSession -> object : FirExpressionResolutionExtension(it) {
                                override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
                                    assertion(functionCall)
                                    return emptyList()
                                }
                            }
                        }
                    }
                })
            }
        }
        useConfigurators(
            ::Subject,
            ::DataFramePluginAnnotationsProvider
        )
    }
}

@Suppress("IncorrectFormatting")
class Test1 : DataFrameUnitTests({ functionCall ->
     val annotation = assertNotNull(functionCall.functionSymbol().annotations.getOrNull(0))
     val name = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()?.classId?.shortClassName
    assertTrue { name!!.identifier == "A" }
 }) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/annotationFromFunctionDeclaration.kt")
    }
}

@Suppress("IncorrectFormatting")
class FindAnnotationOnReceiverFromFirCompiledSources : DataFrameUnitTests({ functionCall ->
     assertTrue {
         val annotation = (functionCall.functionSymbol().resolvedReceiverTypeRef as FirResolvedTypeRef).annotations[0]
         val name = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()!!.classId!!.shortClassName
         name.identifier == "A"
     }
}) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/receiverAnnotationFromFunctionDeclaration.kt")
    }
}

@Suppress("IncorrectFormatting")
class FindAnnotationOnReceiverFromLibrary : DataFrameUnitTests({ functionCall ->
    assertTrue {
        val annotation = functionCall.functionSymbol().annotations[0]
        val name = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()!!.classId!!.shortClassName
        name.identifier == "Schema"
    }
}) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/receiverAnnotationFromFunctionDeclarationFromLibrary.kt")
    }
}