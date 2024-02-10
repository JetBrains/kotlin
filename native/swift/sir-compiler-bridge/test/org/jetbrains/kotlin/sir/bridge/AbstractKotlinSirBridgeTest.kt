/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.*

abstract class AbstractKotlinSirBridgeTest {
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = File(KtTestUtil.getHomeDirectory()).resolve(testPath)
        val expectedKotlinSrc = testPathFull.resolve("expected.kt")
        val expectedCHeader = testPathFull.resolve("expected.h")

        val requests = parseRequestsFromTestDir(testPathFull)

        val generator = createBridgeGenerator()
        val kotlinBridgePrinter = createKotlinBridgePrinter()
        val cBridgePrinter = createCBridgePrinter()

        requests.forEach { request ->
            val bridge = generator.generate(request)
            kotlinBridgePrinter.add(bridge)
            cBridgePrinter.add(bridge)
        }

        val actualKotlinSrc = kotlinBridgePrinter.print().joinToString(separator = lineSeparator)
        val actualHeader = cBridgePrinter.print().joinToString(separator = lineSeparator)

        JUnit5Assertions.assertEqualsToFile(expectedCHeader, actualHeader)
        JUnit5Assertions.assertEqualsToFile(expectedKotlinSrc, actualKotlinSrc)
    }
}

private val lineSeparator: String = System.getProperty("line.separator")

private fun parseRequestsFromTestDir(testDir: File): List<BridgeRequest> =
    testDir.listFiles()
        ?.filter { it.extension == "properties" && it.name.startsWith("request") }
        ?.map { readRequestFromFile(it) }
        ?.sortedBy { it.bridgeName }
        ?: emptyList()

private fun parseType(typeName: String): SirType {
    return when (typeName.lowercase()) {
        "boolean" -> SirSwiftModule.bool

        "byte" -> SirSwiftModule.int8
        "short" -> SirSwiftModule.int16
        "int" -> SirSwiftModule.int32
        "long" -> SirSwiftModule.int64

        "ubyte" -> SirSwiftModule.uint8
        "ushort" -> SirSwiftModule.uint16
        "uint" -> SirSwiftModule.uint32
        "ulong" -> SirSwiftModule.uint64

        else -> error("Unknown type: $typeName")
    }.let { SirNominalType(it) }
}

private fun readRequestFromFile(file: File): BridgeRequest {
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
        BridgeRequestKind.FUNCTION -> buildFunction {
            this.name = fqName.last()
            this.returnType = returnType
            this.parameters += parameters
            this.isStatic = false
        }
        BridgeRequestKind.PROPERTY_GETTER -> {
            val getter = buildGetter()

            getter.parent = buildVariable {
                this.name = fqName.last()
                this.type = returnType
                check(parameters.isEmpty())
                this.isStatic = false
                this.getter = getter
            }

            getter
        }
        BridgeRequestKind.PROPERTY_SETTER -> {
            val setter = buildSetter()

            setter.parent = buildVariable {
                this.name = fqName.last()
                this.type = returnType
                check(parameters.isEmpty())
                this.isStatic = false
                this.getter = buildGetter()
                this.setter = setter
            }

            setter
        }
    }

    return BridgeRequest(callable, bridgeName, fqName)
}

private enum class BridgeRequestKind {
    FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER
}