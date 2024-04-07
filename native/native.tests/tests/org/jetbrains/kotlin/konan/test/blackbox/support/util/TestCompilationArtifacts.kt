/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

private fun invokeKlibTool(
    kotlinNativeClassLoader: ClassLoader,
    klibFile: File,
    command: String,
    printSignatures: Boolean,
    signatureVersion: KotlinIrSignatureVersion?
): String {
    val entryPoint = Class.forName("org.jetbrains.kotlin.cli.klib.Main", true, kotlinNativeClassLoader)
        .declaredMethods
        .single { it.name == "exec" }

    val stdout = StringBuilder()
    val stderr = StringBuilder()

    val args: Array<String> = mutableListOf(
        command, klibFile.canonicalPath,
        "-test-mode", "true",
    ).apply {
        runIf(printSignatures) {
            this += "-print-signatures"
            this += "true"
        }
        signatureVersion?.let {
            this += "-signature-version"
            this += signatureVersion.number.toString()
        }
    }.toTypedArray()

    val exitCode = entryPoint.invoke(null, stdout, stderr, args) as Int
    if (exitCode != 0) {
        error(
            buildString {
                appendLine("Execution of KLIB tool finished with exit code $exitCode")
                args.joinTo(this, prefix = "Arguments: [", postfix = "]\n")
                appendLine()
                appendLine("========== BEGIN: STDOUT ==========")
                append(stdout)
                if (stdout.isNotEmpty() && stdout.last() != '\n') appendLine()
                appendLine("========== END: STDOUT ==========")
                appendLine()
                appendLine("========== BEGIN: STDERR ==========")
                append(stderr)
                if (stderr.isNotEmpty() && stderr.last() != '\n') appendLine()
                appendLine("========== END: STDERR ==========")
            }
        )
    } else {
        return stdout.toString()
    }
}

internal fun TestCompilationArtifact.KLIB.dumpMetadata(
    kotlinNativeClassLoader: ClassLoader,
    printSignatures: Boolean,
    signatureVersion: KotlinIrSignatureVersion?
): String = invokeKlibTool(
    kotlinNativeClassLoader,
    klibFile,
    command = "dump-metadata",
    printSignatures,
    signatureVersion
)

internal fun TestCompilationArtifact.KLIB.dumpIr(
    kotlinNativeClassLoader: ClassLoader,
    printSignatures: Boolean,
    signatureVersion: KotlinIrSignatureVersion?
): String = invokeKlibTool(
    kotlinNativeClassLoader,
    klibFile,
    command = "dump-ir",
    printSignatures,
    signatureVersion
)

internal fun TestCompilationArtifact.KLIB.dumpMetadataSignatures(
    kotlinNativeClassLoader: ClassLoader,
    signatureVersion: KotlinIrSignatureVersion,
): String = invokeKlibTool(
    kotlinNativeClassLoader,
    klibFile,
    command = "dump-metadata-signatures",
    printSignatures = false,
    signatureVersion
)

internal fun TestCompilationArtifact.KLIB.dumpIrSignatures(
    kotlinNativeClassLoader: ClassLoader,
    signatureVersion: KotlinIrSignatureVersion,
): String = invokeKlibTool(
    kotlinNativeClassLoader,
    klibFile,
    command = "dump-ir-signatures",
    printSignatures = false,
    signatureVersion
)
