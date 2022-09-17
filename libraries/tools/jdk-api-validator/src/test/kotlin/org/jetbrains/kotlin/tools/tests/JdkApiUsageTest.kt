/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import org.codehaus.mojo.animal_sniffer.*
import org.codehaus.mojo.animal_sniffer.logging.Logger
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger
import java.io.File
import kotlin.test.*

class JdkApiUsageTest {

    @Test
    fun kotlinStdlib() {
        testApiUsage(
            jarArtifact("../../stdlib/jvm/build/libs", "kotlin-stdlib-jdk6"),
            dependencies = listOf()
        )
    }

    @Test
    fun kotlinReflect() {
        testApiUsage(
            jarArtifact("../../reflect/build/libs", "kotlin-reflect"),
            dependencies = listOf(jarArtifact("../../stdlib/jvm/build/libs", "kotlin-stdlib"))
        )
    }

    private fun testApiUsage(artifact: File, dependencies: List<File>) {
        val logger = TestLogger()
        val additionalArtifacts = buildList {
            add(artifact)
            addAll(dependencies)
        }

        val signatures = buildSignatures(additionalArtifacts, logger)
        if (logger.hasError) {
            fail("Building signatures has failed. See console logs for details.")
        }

        checkSignatures(artifact, signatures, logger)
        if (logger.hasError) {
            fail("Checking signatures has failed. See console logs for details.")
        }
    }

    private fun checkSignatures(artifact: File, signatures: File, logger: Logger) {
        val checker = SignatureChecker(signatures.inputStream(), emptySet(), logger)
        checker.setSourcePath(emptyList())
        checker.setAnnotationTypes(suppressAnnotations)
        checker.process(artifact)
    }

    private fun buildSignatures(additionalArtifacts: List<File>, logger: Logger): File {
        val signaturesDirectory = System.getProperty("signaturesDirectory")
            .let { requireNotNull(it) { "Specify signaturesDirectory with a system property" } }
            .let { File(it) }

        val mergedSignaturesDirectory = signaturesDirectory.resolveSibling("signatures-merged").also { it.mkdirs() }
        val mergedSignaturesFile = File.createTempFile("merged", null, mergedSignaturesDirectory)

        val signatureInputStreams = (signaturesDirectory.listFiles() ?: throw Exception("Cannot list files in $signaturesDirectory"))
            .map { it.inputStream() }
        val mergedSignaturesOutputStream = mergedSignaturesFile.outputStream()

        val signatureBuilder = SignatureBuilder(signatureInputStreams.toTypedArray(), mergedSignaturesOutputStream, logger)
        additionalArtifacts.forEach {
            signatureBuilder.process(it)
        }
        signatureBuilder.close()

        return mergedSignaturesFile
    }

    private fun jarArtifact(basePath: String, jarPattern: String): File {
        val kotlinVersion = System.getProperty("kotlinVersion")
            .let { requireNotNull(it) { "Specify kotlinVersion with a system property" } }

        val versionPattern = "-" + Regex.escape(kotlinVersion)
        val regex = Regex("$jarPattern$versionPattern\\.jar")
        val base = File(basePath).absoluteFile.normalize()
        val files = (base.listFiles() ?: throw Exception("Cannot list files in $base"))
            .filter { it.name matches regex }

        return files.singleOrNull() ?: throw Exception("No single file matching $regex in $base:\n${files.joinToString("\n")}")
    }
}

private val suppressAnnotations = listOf(
    "kotlin.reflect.jvm.internal.SuppressAnimalSniffer",
    "kotlin.internal.SuppressAnimalSniffer",
)

private val undefinedReferencesToIgnore = listOf(
    "int Integer.compareUnsigned(int, int)",
    "int Integer.remainderUnsigned(int, int)",
    "int Integer.divideUnsigned(int, int)",

    "int Long.compareUnsigned(long, long)",
    "long Long.remainderUnsigned(long, long)",
    "long Long.divideUnsigned(long, long)",

    "int Byte.hashCode(byte)",
    "int Short.hashCode(short)",
    "int Integer.hashCode(int)",
    "int Long.hashCode(long)",
    "int Float.hashCode(float)",
    "int Double.hashCode(double)",
)

private class TestLogger : Logger {
    private val logger = PrintWriterLogger(System.out)

    var hasError: Boolean = false
        private set

    override fun info(message: String) = logger.info(message)
    override fun info(message: String, t: Throwable?) = logger.info(message, t)

    override fun debug(message: String) = logger.debug(message)
    override fun debug(message: String, t: Throwable?) = logger.debug(message, t)

    override fun warn(message: String) = logger.warn(message)
    override fun warn(message: String, t: Throwable?) = logger.warn(message, t)

    override fun error(message: String) = error(message, null)
    override fun error(message: String, t: Throwable?) {
        val shouldIgnore = undefinedReferencesToIgnore.any {
            message.endsWith("Undefined reference: $it")
        }
        if (shouldIgnore) {
            return
        }

        hasError = true
        logger.error(message, t)
    }
}