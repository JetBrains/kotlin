/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.unit

import org.jetbrains.dataframe.impl.codeGen.CodeGenerator
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.*
import org.jetbrains.kotlin.fir.dataframe.extensions.FirDataFrameExtensionsGenerator
import org.jetbrains.kotlin.fir.dataframe.extensions.SchemaContext
import org.jetbrains.kotlin.fir.dataframe.services.BaseTestRunner
import org.jetbrains.kotlin.fir.dataframe.services.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacadeImpl
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.plugin.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText

abstract class TestWithCompileTimeInformation : BaseTestRunner() {
    lateinit var filePath: String

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
        }

        useConfigurators(
            ::DataFramePluginAnnotationsProvider,
            { testServices: TestServices -> Configurator(testServices, { filePath }, ::setTestSubject, ::onCompile) },
        )
    }

    override fun runTest(filePath: String) {
        this.filePath = filePath
        super.runTest(filePath)
    }

    abstract fun setTestSubject(subject: PluginDataFrameSchema)

    abstract fun onCompile(session: FirSession)

    class Configurator(
        testServices: TestServices,
        val getTestFilePath: () -> String,
        val setTestSubject: (PluginDataFrameSchema) -> Unit,
        val onCompile: (FirSession) -> Unit
    ) : EnvironmentConfigurator(testServices) {
        override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
            module: TestModule,
            configuration: CompilerConfiguration
        ) {
            FirExtensionRegistrarAdapter.registerExtension(object : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    with(GeneratedNames()) {
                        +::FirDataFrameExtensionsGenerator
                        +{ it: FirSession -> InterpretersRunner(it, tokenState, getTestFilePath, setTestSubject, onCompile) }
                    }
                }
            })
        }
    }

    class InterpretersRunner(
        session: FirSession,
        val tokenState: MutableMap<ClassId, SchemaContext>,
        val getTestFilePath: () -> String,
        val setTestSubject: (PluginDataFrameSchema) -> Unit,
        val onCompile: (FirSession) -> Unit
    ) : FirExpressionResolutionExtension(session), KotlinTypeFacade {
        override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
            functionCall.calleeReference.name.identifierOrNullIfSpecial?.let {
                if (it == "test") {
                    val call = functionCall.arguments[1].unwrapArgument() as FirFunctionCall
                    val interpreter = call.loadInterpreter()!!
                    val result = interpret(call, interpreter, reporter = { _, _ -> })?.value ?: TODO("test error cases")

                    setTestSubject(result as PluginDataFrameSchema)
                    onCompile(session)
                }
            }
            return emptyList()
        }
    }
}

class Explode : TestWithCompileTimeInformation() {
    val compilationDir: Path = Paths.get("build/test-compile").toAbsolutePath()

    val codegen = CodeGenerator.create()
    lateinit var schema: PluginDataFrameSchema
    lateinit var compilationCallback: (FirSession) -> Unit

    override fun setTestSubject(subject: PluginDataFrameSchema) {
        schema = subject
    }

    override fun onCompile(session: FirSession) {
        compilationCallback(session)
    }

    val df = dataFrameOf("name", "age", "city", "weight")(
        "Alice", 15, "London", 54,
        "Bob", 45, "Dubai", 87,
        "Charlie", 20, "Moscow", null,
        "Charlie", 40, "Milan", null,
        "Bob", 30, "Tokyo", 68,
        "Alice", 20, null, 55,
        "Charlie", 30, "Moscow", 90
    )

    private val defaultExplodeColumns: ColumnsSelector<*, *> = { dfs { it.isList() || it.isFrameColumn() } }

    fun <T> DataFrame<T>.explodeTest(
        facade: KotlinTypeFacade,
        dropEmpty: Boolean = true,
        selector: ColumnsSelector<T, *> = defaultExplodeColumns,
        pluginDataFrameSchema: PluginDataFrameSchema
    ): DataFrame<T> {
        val df = this
        println("Before runtime")
        schema().print()
        println("Before compile")
        facade.pluginSchema(this).print()
        val runtime = explode(dropEmpty, selector)
        println()
        println()
        println("Runtime")
        runtime.schema().print()
        println("Compile")
        val compile = facade.run { pluginDataFrameSchema.explodeImpl(dropEmpty, selector.toColumnPath(df)) }
        compile.print()
        return runtime
    }

    lateinit var file: Path

    @BeforeEach
    fun createFile() {
        if (!compilationDir.exists()) {
            compilationDir.createDirectory()
        }

        file = kotlin.io.path.createTempFile(compilationDir)
    }

    @Test
    fun test() {
        val grouped = df
            .filter { it["city"] != null }
            .remove("age", "weight")
            .groupBy("city")
            .toDataFrame()

        dumpSchema(grouped)
        //File("plugins/kotlin-dataframe/testData/unit/dummy.kt").writeText()

        compilationCallback = {
            val facade = KotlinTypeFacadeImpl(it)
            grouped.explodeTest(facade, dropEmpty = true, pluginDataFrameSchema = schema)
        }

        runTest(file.absolutePathString())
    }

    @Test
    fun test1() {
        val df = dataFrameOf("packageName", "files")(
            "org.jetbrains.kotlinx.dataframe.api", listOf("add.kt", "addId.kt", "all.kt"),
            "org.jetbrains.kotlinx.dataframe.io", listOf("common.kt", "csv.kt", "guess.kt")
        )

        dumpSchema(df)

        compilationCallback = {
            val facade = KotlinTypeFacadeImpl(it)
            df.explodeTest(facade, dropEmpty = true, pluginDataFrameSchema = schema)
        }

        runTest(file.absolutePathString())
    }

    private fun dumpSchema(grouped: DataFrame<Any?>) {
        val schemaDeclaration =
            codegen.generate(grouped.schema(), "Schema", true, false, false)


        file.writeText(
            """
                import org.jetbrains.kotlinx.dataframe.*
                import org.jetbrains.kotlinx.dataframe.api.*
                import org.jetbrains.kotlinx.dataframe.annotations.*
                import org.jetbrains.kotlinx.dataframe.plugin.testing.*
                import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

                ${schemaDeclaration.code.declarations}

                fun dummy(df: DataFrame<Schema>) {
                    test(id = "dummy", call = dataFrame(df))
                }
            """.trimIndent()
        )
    }

}
