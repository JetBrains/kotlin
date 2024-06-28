/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import org.codehaus.mojo.animal_sniffer.*
import org.codehaus.mojo.animal_sniffer.logging.Logger
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class JdkApiUsageTest {

    @Test
    fun kotlinReflect() {
        testApiUsage(
            // TODO: try to pass these paths from Gradle
            // Do not use the final jar artifact. It's shrunk by proguard.
            jarArtifact("../../reflect/build/libs", "kotlin-reflect", "shadow"),
            dependencies = listOf(jarArtifact("../../stdlib/build/libs", "kotlin-stdlib"))
        )
    }

    private fun testApiUsage(artifact: Path, dependencies: List<Path>) {
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
            fail("Checking signatures has failed. See console logs for details. "
                         + "See libraries/tools/jdk-api-validator/ReadMe.md to find out how to fix the failures.")
        }
    }

    private fun checkSignatures(artifact: Path, signatures: Path, logger: Logger) {
        val checker = SignatureChecker(signatures.inputStream(), emptySet(), logger)
        checker.setSourcePath(emptyList())
        checker.setAnnotationTypes(suppressAnnotations)
        checker.process(artifact.toFile())  // the overload that takes Path can't handle jar files
    }

    private fun buildSignatures(additionalArtifacts: List<Path>, logger: Logger): Path {
        val signaturesDirectory = System.getProperty("signaturesDirectory")
            .let { requireNotNull(it) { "Specify signaturesDirectory with a system property" } }
            .let { Path(it) }

        val mergedSignaturesDirectory = signaturesDirectory.resolveSibling("signatures-merged").createDirectories()
        val mergedSignaturesFile = createTempFile(mergedSignaturesDirectory, "merged", null)

        val signatureInputStreams = signaturesDirectory.listDirectoryEntries()
            .map { it.inputStream() }
        val mergedSignaturesOutputStream = mergedSignaturesFile.outputStream()

        val signatureBuilder = SignatureBuilder(signatureInputStreams.toTypedArray(), mergedSignaturesOutputStream, logger)
        try {
            additionalArtifacts.forEach {
                signatureBuilder.process(it.toFile()) // the overload that takes Path can't handle jar files
            }
        } finally {
            signatureBuilder.close()
        }

        return mergedSignaturesFile
    }

    private fun jarArtifact(basePath: String, jarBaseName: String, jarClassifier: String? = null): Path {
        val kotlinVersion = System.getProperty("kotlinVersion")
            .let { requireNotNull(it) { "Specify kotlinVersion with a system property" } }

        val jarFullName = "$jarBaseName-$kotlinVersion${jarClassifier?.let { "-$it" } ?: ""}.jar"
        val base = Path(basePath).absolute().normalize()
        val file = base.listDirectoryEntries()
            .firstOrNull { it.name == jarFullName }

        return file ?: throw Exception("No file with name $jarFullName in $base")
    }
}

private val suppressAnnotations = listOf(
    "kotlin.reflect.jvm.internal.SuppressJdk6SignatureCheck",

    // The following two fqn refer to the same annotation. The first is before its relocation,
    // the second is after. See kotlin-reflect build script.
    "org.jetbrains.kotlin.SuppressJdk6SignatureCheck",
    "kotlin.reflect.jvm.internal.impl.SuppressJdk6SignatureCheck",
)

private val undefinedReferencesToIgnore = listOf(
    "int Integer.compareUnsigned(int, int)",
    "int Integer.remainderUnsigned(int, int)",
    "int Integer.divideUnsigned(int, int)",

    "int Long.compareUnsigned(long, long)",
    "long Long.remainderUnsigned(long, long)",
    "long Long.divideUnsigned(long, long)",

    "int Boolean.hashCode(boolean)",
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