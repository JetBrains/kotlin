/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import java.io.File

fun invokeKlibTool(
    kotlinNativeClassLoader: ClassLoader,
    args: List<String>,
): Pair<Int, String> {
    val entryPoint = Class.forName("org.jetbrains.kotlin.cli.klib.Main", true, kotlinNativeClassLoader)
        .declaredMethods
        .single { it.name == "exec" }

    val stdout = StringBuilder()
    val stderr = StringBuilder()

    val exitCode = entryPoint.invoke(null, stdout, stderr, args.toTypedArray()) as Int
    val output = stdout.toString() + stderr.toString()

    return exitCode to output
}

private fun invokeKlibTool(
    kotlinNativeClassLoader: ClassLoader,
    klibFile: File,
    command: String,
    metadataTestMode: String? = null,
    signatureVersion: KotlinIrSignatureVersion? = null,
    signatureDumpMode: String? = null,
    absolutePathPrefixes: List<String> = emptyList(),
): String {
    val args = buildList<String> {
        this += command
        this += klibFile.canonicalPath
        metadataTestMode?.let {
            this += "-dump-metadata-test-mode"
            this += metadataTestMode
        }
        signatureVersion?.let {
            this += "-signature-version"
            this += signatureVersion.number.toString()
        }
        signatureDumpMode?.let {
            this += "-dump-signatures-mode"
            this += it
        }
        absolutePathPrefixes.forEach {
            this += "-relative-path-base"
            this += it
        }
    }

    val [exitCode, output] = invokeKlibTool(kotlinNativeClassLoader, args)

    if (exitCode != 0) {
        error(
            buildString {
                appendLine("Execution of KLIB tool finished with exit code $exitCode")
                args.joinTo(this, prefix = "Arguments: [", postfix = "]\n")
                appendLine()
                appendLine("========== BEGIN: OUTPUT ==========")
                append(output)
                if (output.isNotEmpty() && output.last() != '\n') appendLine()
                appendLine("========== END: OUTPUT ==========")
            }
        )
    }

    return output
}

fun TestCompilationArtifact.KLIB.dumpMetadata(
    kotlinNativeClassLoader: ClassLoader,
    metadataTestMode: String? = "compact-with-stable-order",
): String = klibFile.dumpMetadata(kotlinNativeClassLoader, metadataTestMode)

fun File.dumpMetadata(
    kotlinNativeClassLoader: ClassLoader,
    metadataTestMode: String? = "compact-with-stable-order",
): String = invokeKlibTool(
    kotlinNativeClassLoader,
    klibFile = this,
    command = "dump-metadata",
    metadataTestMode = metadataTestMode,
)

fun TestCompilationArtifact.KLIB.dumpIr(
    kotlinNativeClassLoader: ClassLoader,
    absolutePathPrefixes: List<String> = emptyList(),
): String = klibFile.dumpIr(kotlinNativeClassLoader, absolutePathPrefixes)

fun File.dumpIr(
    kotlinNativeClassLoader: ClassLoader,
    absolutePathPrefixes: List<String> = emptyList(),
): String = invokeKlibTool(
    kotlinNativeClassLoader = kotlinNativeClassLoader,
    klibFile = this,
    command = "dump-ir",
    absolutePathPrefixes = absolutePathPrefixes,
)

fun TestCompilationArtifact.KLIB.dumpSignatures(
    kotlinNativeClassLoader: ClassLoader,
    signatureVersion: KotlinIrSignatureVersion,
    signatureDumpMode: String,
): String = klibFile.dumpSignatures(
    kotlinNativeClassLoader = kotlinNativeClassLoader,
    signatureVersion = signatureVersion,
    signatureDumpMode = signatureDumpMode,
)

fun File.dumpSignatures(
    kotlinNativeClassLoader: ClassLoader,
    signatureVersion: KotlinIrSignatureVersion,
    signatureDumpMode: String,
): String = invokeKlibTool(
    kotlinNativeClassLoader = kotlinNativeClassLoader,
    klibFile = this,
    command = "dump-signatures",
    signatureVersion = signatureVersion,
    signatureDumpMode = signatureDumpMode,
)
