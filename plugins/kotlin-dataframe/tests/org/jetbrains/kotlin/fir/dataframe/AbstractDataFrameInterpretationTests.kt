/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.services.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximationImpl
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.plugin.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.Context

abstract class AbstractDataFrameInterpretationTests : AbstractKotlinCompilerTest() {
    lateinit var filePath: String

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
            +FIR_DUMP
        }

        useConfigurators(
            ::DataFramePluginAnnotationsProvider,
            { testServices: TestServices -> Configurator(testServices, { filePath }) },
        )
    }

    override fun runTest(filePath: String) {
        this.filePath = filePath
        super.runTest(filePath)
    }

    class Configurator(testServices: TestServices, val getTestFilePath: () -> String) : EnvironmentConfigurator(testServices) {
        override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
            module: TestModule,
            configuration: CompilerConfiguration
        ) {

            val ids = List(100) {
                val name = Name.identifier(it.toString())
                ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), name)
            }.toSet()
            val queue = ArrayDeque(ids)
            val state = mutableMapOf<ClassId, SchemaContext>()

            FirExtensionRegistrarAdapter.registerExtension(object : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    +{ it: FirSession -> FirDataFrameExtensionsGenerator(it, ids, state) }
                    +{ it: FirSession -> InterpretersRunner(it, queue, state, getTestFilePath) }
                }
            })
        }
    }

    class InterpretersRunner(
        session: FirSession,
        val queue: ArrayDeque<ClassId>,
        val state: MutableMap<ClassId, SchemaContext>,
        val getTestFilePath: () -> String
    ) : FirExpressionResolutionExtension(session) {
        override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
            functionCall.calleeReference.name.identifierOrNullIfSpecial?.let {
                if (it == "test") {
                    val id = (functionCall.arguments[0].unwrapArgument() as FirConstExpression<*>).value as String
                    val call = functionCall.arguments[1].unwrapArgument() as FirFunctionCall
                    val interpreter = call.loadInterpreter()!!
                    val result = interpret(call, interpreter)?.value ?: TODO("test error cases")

                    withClue(id) {
                        result shouldBe expectedResult(id)
                    }
                }
            }
            val file = getTestFilePath()
            val rootMarkerStrategy = when {
                file.contains("convert") -> any
                else -> id
            }
            return coneKotlinTypes(functionCall, state, queue, rootMarkerStrategy)
        }

        fun expectedResult(id: String): Any? {
            val map = mapOf(
                "string_1" to "42",
                "string_2" to "42",
                "dataFrame_1" to PluginDataFrameSchema(listOf(SimpleCol("i", TypeApproximationImpl("kotlin.Int", false)))),
                "dataFrame_2" to PluginDataFrameSchema(emptyList()),
                "type_1" to TypeApproximationImpl("kotlin.Int", nullable = false),
                "insert_1" to InsertClauseApproximation(
                    PluginDataFrameSchema(columns = emptyList()),
                    SimpleCol("b", TypeApproximationImpl("kotlin.Int", false))
                ),
                "enum_1" to Infer.Type,
                "kproperty_1" to KPropertyApproximation("i", TypeApproximationImpl("kotlin.Int", false)),
                "kproperty_2" to KPropertyApproximation("name", TypeApproximationImpl("kotlin.Int", false)),
                "addExpression_1" to TypeApproximationImpl("kotlin.Int", nullable = false),
                "addExpression_2" to TypeApproximationImpl("kotlin.Any", nullable = true),
                "add0_schema" to pluginJsonFormat.decodeFromString<PluginDataFrameSchema>("""{"columns":[{"type":"org.jetbrains.kotlinx.dataframe.plugin.SimpleCol","name":"a","valuesType":{"type":"org.jetbrains.kotlinx.dataframe.annotations.TypeApproximationImpl","fqName":"kotlin.Int","nullable":false}}]}"""),
                "add0" to pluginJsonFormat.decodeFromString<PluginDataFrameSchema>("""{"columns":[{"type":"org.jetbrains.kotlinx.dataframe.plugin.SimpleCol","name":"a","valuesType":{"type":"org.jetbrains.kotlinx.dataframe.annotations.TypeApproximationImpl","fqName":"kotlin.Int","nullable":false}},{"type":"org.jetbrains.kotlinx.dataframe.plugin.SimpleCol","name":"untitled","valuesType":{"type":"org.jetbrains.kotlinx.dataframe.annotations.TypeApproximationImpl","fqName":"kotlin.Int","nullable":false}}]}"""),
                "varargKProperty_0" to listOf(
                    KPropertyApproximation("col1", TypeApproximationImpl("kotlin.Int", false)),
                    KPropertyApproximation("col2", TypeApproximationImpl("kotlin.Int", true))
                ),
                "memberFunction_1" to Context(123),
                "typeParameter_1" to TypeApproximationImpl("kotlin.Int", false),
                "rowValueExpression_1" to TypeApproximationImpl("kotlin.Int", nullable = false)
            )
            return map[id]
        }
    }
}