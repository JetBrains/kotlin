/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting")

package org.jetbrains.kotlin.fir.dataframe.unit

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.dataframe.functionSymbol
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

abstract class DataFrameUnitTests(val assertion: List<(FirFunctionCall) -> Unit>) : AbstractKotlinCompilerTest() {

    constructor(assertion: (FirFunctionCall) -> Unit): this(listOf(assertion))

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
                                var i = 0
                                override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
                                    if (assertion.size == 1) {
                                        assertion[0](functionCall)
                                    } else {
                                        assertion[i](functionCall)
                                        i++
                                    }
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

class CallWithVariable : DataFrameUnitTests({ functionCall ->
    val argument = functionCall.arguments[0]
    assertIs<FirPropertyAccessExpression>(argument)
}) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/callWithVariable.kt")
    }
}

class CallWithReference : DataFrameUnitTests(
    listOf({ functionCall ->
        val argument = functionCall.arguments[0]
        assertIs<FirCallableReferenceAccess>(argument)
        assertTrue { argument.calleeReference.name == Name.identifier("i") }
    }, { functionCall ->
        val argument = functionCall.arguments[0]
        assertIs<FirCallableReferenceAccess>(argument)
        val callableId = (argument.calleeReference.resolvedSymbol as? FirCallableSymbol)?.callableId
        assertNotNull(callableId)
        assertTrue { callableId.callableName == Name.identifier("i") }
        assertTrue { callableId.className?.shortName() == Name.identifier("Schema") }
    }, { functionCall ->
        val argument = functionCall.arguments[0]
        assertIs<FirCallableReferenceAccess>(argument)
        argument.calleeReference.resolvedSymbol?.resolvedAnnotationsWithArguments?.isNotEmpty()
    })
) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/callWithReference.kt")
    }
}

class AccessReceiverInCallChain : DataFrameUnitTests(lambda@{ functionCall ->
//    val callReturnType = functionCall.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return@lambda
//    if (callReturnType.classId != FirDataFrameReceiverInjector.DF_CLASS_ID) return@lambda
    println(functionCall)
    val ff = f(functionCall)
    println(ff)
}) {

    @Test
    fun test() {
        runTest("plugins/kotlin-dataframe/testData/unit/accessReceiverInCallChain.kt")
    }
}

fun f(parent: FirQualifiedAccess): FirPropertyAccessExpression {
    return when (val er = parent.explicitReceiver) {
        null -> parent as FirPropertyAccessExpression
        is FirQualifiedAccess -> { f(er) }
        else -> TODO("${er::class}")
    }
}
