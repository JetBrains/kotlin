/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import com.intellij.testFramework.TestDataFile
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

private fun parseType(typeName: String): BridgeRequest.Type {
    return when (typeName.lowercase()) {
        "boolean" -> BridgeRequest.Type.Boolean

        "byte" -> BridgeRequest.Type.Byte
        "short" -> BridgeRequest.Type.Short
        "int" -> BridgeRequest.Type.Int
        "long" -> BridgeRequest.Type.Long

        "ubyte" -> BridgeRequest.Type.UByte
        "ushort" -> BridgeRequest.Type.UShort
        "uint" -> BridgeRequest.Type.UInt
        "ulong" -> BridgeRequest.Type.ULong

        else -> error("Unknown type: $typeName")
    }
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
            BridgeRequest.Parameter(
                name = it.substringBefore(':'),
                type = parseType(it.substringAfter(':'))
            )
        }
    }
    return BridgeRequest(fqName, bridgeName, parameters, returnType)
}