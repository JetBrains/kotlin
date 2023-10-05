/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import java.io.File

private fun invokeKlibTool(kotlinNativeClassLoader: ClassLoader, klibFile: File, functionName: String, vararg args: Any?): String {
    val libraryClass = Class.forName("org.jetbrains.kotlin.cli.klib.Library", true, kotlinNativeClassLoader)
    val entryPoint = libraryClass.declaredMethods.single { it.name == functionName }
    val lib = libraryClass.getDeclaredConstructor(String::class.java, String::class.java).newInstance(klibFile.canonicalPath, null)

    val output = StringBuilder()
    entryPoint.invoke(lib, output, *args)
    return output.toString()

}

internal fun TestCompilationArtifact.KLIB.dumpMetadata(kotlinNativeClassLoader: ClassLoader): String {
    return invokeKlibTool(kotlinNativeClassLoader, klibFile, "dumpMetadata", /* printSignatures= */ false)
}

internal fun TestCompilationArtifact.KLIB.dumpIr(
    kotlinNativeClassLoader: ClassLoader,
    printSignatures: Boolean = false,
): String {
    return invokeKlibTool(kotlinNativeClassLoader, klibFile, "dumpIr", printSignatures)
}

internal fun TestCompilationArtifact.KLIB.dumpIrSignatures(
    kotlinNativeClassLoader: ClassLoader,
    signatureVersion: KotlinIrSignatureVersion,
): String {
    // This ceremony is required to load `KotlinIrSignatureVersion` class from the isolated class loader and thus avoid
    // "argument type mismatch" exception raised by the Java reflection API.
    // TODO: migrate on CLI-based scheme of invocation of all KLIB tool commands
    val signatureVersionForIsolatedClassLoader = Class.forName(
        signatureVersion::class.java.canonicalName,
        true,
        kotlinNativeClassLoader
    ).getDeclaredConstructor(Int::class.java).newInstance(signatureVersion.number)!!

    return invokeKlibTool(kotlinNativeClassLoader, klibFile, "dumpIrSignatures", signatureVersionForIsolatedClassLoader)
}
