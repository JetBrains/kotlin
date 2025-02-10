/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysisImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.providers.source.KotlinPropertyAccessorOrigin
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.Files
import java.util.*

abstract class AbstractKotlinSirBridgeTest {
    /**
     * TODO: Tests should be simpler. See KT-75083.
     */
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = File(KtTestUtil.getHomeDirectory()).resolve(testPath)
        val tempDirectory = Files.createTempDirectory("bridgesTest_${testPathFull.name}").toFile()
        try {
            val codeAnalysis = InlineSourceCodeAnalysisImpl(tempDirectory)
            val expectedKotlinSrc = testPathFull.resolve("expected.kt")
            val expectedCHeader = testPathFull.resolve("expected.h")
            val ktFile = codeAnalysis.createKtFile(
                """
            fun dummyFunction() {}

            var dummyProperty: Int = 0
        """.trimIndent()
            )
            analyze(ktFile) {
                val testHelper = TemporaryTestHelper(ktFile, this)
                val requests = parseRequestsFromTestDir(testPathFull, testHelper)

                val generator = createBridgeGenerator(object : SirTypeNamer {
                    override fun swiftFqName(type: SirType): String {
                        return when (type) {
                            is SirNominalType -> {
                                require(type.typeDeclaration.origin is SirOrigin.ExternallyDefined)
                                type.typeDeclaration.name
                            }
                            is SirFunctionalType -> {
                                // todo: KT-72993
                                (listOf("function") + type.parameterTypes.map { swiftFqName(it) }).joinToString("_")
                            }
                            else -> error("Unsupported type: $type")
                        }
                    }

                    override fun kotlinFqName(type: SirType): String {
                        return when (type) {
                            is SirNominalType -> {
                                require(type.typeDeclaration.origin is SirOrigin.ExternallyDefined)
                                type.typeDeclaration.name
                            }
                            is SirFunctionalType -> {
                                // todo: KT-72993
                                (listOf("function") + type.parameterTypes.map { kotlinFqName(it) }).joinToString("_")
                            }
                            else -> error("Unsupported type: $type")
                        }
                    }
                })
                val kotlinBridgePrinter = createKotlinBridgePrinter()
                val cBridgePrinter = createCBridgePrinter()

                requests.forEach { request ->
                    generator.generateBridges(request).forEach {
                        kotlinBridgePrinter.add(it)
                        cBridgePrinter.add(it)
                    }
                }

                val actualKotlinSrc = kotlinBridgePrinter.print().joinToString(separator = lineSeparator)
                val actualHeader = cBridgePrinter.print().joinToString(separator = lineSeparator)

                JUnit5Assertions.assertEqualsToFile(expectedCHeader, actualHeader)
                JUnit5Assertions.assertEqualsToFile(expectedKotlinSrc, actualKotlinSrc)
            }
        } finally {
            tempDirectory.deleteRecursively()
        }
    }
}

private val lineSeparator: String = System.getProperty("line.separator")

private fun parseRequestsFromTestDir(testDir: File, testHelper: TemporaryTestHelper): List<FunctionBridgeRequest> =
    testDir.listFiles()
        ?.filter { it.extension == "properties" && it.name.startsWith("request") }
        ?.map { readRequestFromFile(it, testHelper) }
        ?.sortedWith(StableBridgeRequestComparator)
        ?: emptyList()

private fun parseType(typeName: String): SirType {
    val tn = typeName.lowercase()

    return if (tn.startsWith("closure")) {
        when (tn) {
            "closure_void" -> {
                SirFunctionalType(
                    returnType = SirNominalType(SirSwiftModule.void),
                    parameterTypes = emptyList(),
                )
            }
            "closure_closure" -> {
                SirFunctionalType(
                    returnType = SirFunctionalType(
                        returnType = SirNominalType(SirSwiftModule.void),
                        parameterTypes = emptyList(),
                    ),
                    parameterTypes = emptyList(),
                )
            }
            "closure_with_parameter" -> {
                SirFunctionalType(
                    returnType = SirNominalType(buildClass {
                        name = "MyClass"
                        origin = SirOrigin.ExternallyDefined(name = "MyClass")
                    }),
                    parameterTypes = listOf(SirNominalType(buildClass {
                        name = "MyAnotherClass"
                        origin = SirOrigin.ExternallyDefined(name = "MyAnotherClass")
                    })),
                )
            }
            else -> error("unknown tag for predefined closure")
        }
    } else {
        when (tn) {
            "boolean" -> SirSwiftModule.bool

            "byte" -> SirSwiftModule.int8
            "short" -> SirSwiftModule.int16
            "int" -> SirSwiftModule.int32
            "long" -> SirSwiftModule.int64

            "ubyte" -> SirSwiftModule.uint8
            "ushort" -> SirSwiftModule.uint16
            "uint" -> SirSwiftModule.uint32
            "ulong" -> SirSwiftModule.uint64

            "void" -> SirSwiftModule.void

            "any" -> buildClass {
                name = "MyClass"
                origin = SirOrigin.ExternallyDefined(name = "MyClass")
            }

            else -> error("Unknown type: $typeName")
        }.let { SirNominalType(it) }
    }
}

// A temporary solution to provide Kotlin origins to Swift declarations.
// A proper solution would be to SIR session instead.
private class TemporaryTestHelper(
    private val ktFile: KtFile,
    private val kaSession: KaSession,
) {

    private inline fun findProperty(filter: (KaPropertySymbol) -> Boolean = { true }) = with(kaSession) {
        ktFile.symbol.fileScope.declarations.filterIsInstance<KaPropertySymbol>().first(filter)
    }

    fun propertyGetterOrigin(): Pair<KotlinSource, KotlinPropertyAccessorOrigin> = with(kaSession) {
        val property = findProperty { it.getter != null }
        KotlinSource(property) to KotlinPropertyAccessorOrigin(
            property.getter!!,
            property
        )
    }

    fun propertySetterOrigin(): Pair<KotlinSource, KotlinPropertyAccessorOrigin> = with(kaSession) {
        val property = findProperty { it.setter != null }
        KotlinSource(property) to KotlinPropertyAccessorOrigin(
            property.setter!!,
            property
        )
    }

    fun functionOrigin(): KotlinSource = with(kaSession) {
        KotlinSource(ktFile.symbol.fileScope.declarations.filterIsInstance<KaNamedFunctionSymbol>().first())
    }
}

private fun readRequestFromFile(file: File, testHelper: TemporaryTestHelper): FunctionBridgeRequest {
    val properties = Properties()
    file.bufferedReader().use(properties::load)
    val fqName = properties.getProperty("fqName").split('.')
    val bridgeName = properties.getProperty("bridgeName")
    val returnType = parseType(properties.getProperty("returnType"))
    val parameters = properties.getProperty("parameters").let { parametersString ->
        when {
            parametersString.isNullOrEmpty() -> emptyList()
            else -> parametersString.split(Regex("\\s+"))
        }.map {
            SirParameter(
                argumentName = it.substringBefore(':'),
                type = parseType(it.substringAfter(':'))
            )
        }
    }

    val kind = BridgeRequestKind.valueOf(properties.getProperty("kind", "FUNCTION"))

    val callable = when (kind) {
        BridgeRequestKind.FUNCTION -> {
            val function = buildFunction {
                this.name = fqName.last()
                this.returnType = returnType
                this.parameters += parameters
                this.origin = testHelper.functionOrigin()
            }

            buildModule {
                name = "BridgeTest"
            }.apply {
                addChild { function }
            }

            function
        }
        BridgeRequestKind.PROPERTY_GETTER -> {
            val (propertyOrigin, getterOrigin) = testHelper.propertyGetterOrigin()
            val getter = buildGetter {
                this.origin = getterOrigin
            }

            val variable = buildVariable {
                this.name = fqName.last()
                this.type = returnType
                check(parameters.isEmpty())
                this.getter = getter
                this.origin = propertyOrigin
            }

            getter.parent = variable

            buildModule {
                name = "BridgeTest"
            }.apply {
                addChild { variable }
            }

            getter
        }
        BridgeRequestKind.PROPERTY_SETTER -> {
            val (propertyOrigin, setterOrigin) = testHelper.propertySetterOrigin()
            val setter = buildSetter {
                origin = setterOrigin
            }

            val variable = buildVariable {
                this.name = fqName.last()
                this.type = returnType
                check(parameters.isEmpty())
                this.getter = buildGetter {}
                this.setter = setter
                this.origin = propertyOrigin
            }

            setter.parent = variable

            buildModule {
                name = "BridgeTest"
            }.apply {
                addChild { variable }
            }

            setter
        }
    }

    return FunctionBridgeRequest(callable, bridgeName, fqName)
}

private enum class BridgeRequestKind {
    FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER
}