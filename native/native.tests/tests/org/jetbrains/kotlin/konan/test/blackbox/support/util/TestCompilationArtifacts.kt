/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import java.io.File

private fun invokeKlibTool(kotlinNativeClassLoader: ClassLoader, klibFile: File, functionName: String, vararg args: Any): String {
    val libraryClass = Class.forName("org.jetbrains.kotlin.cli.klib.Library", true, kotlinNativeClassLoader)
    val entryPoint = libraryClass.declaredMethods.single { it.name == functionName }
    val lib = libraryClass.getDeclaredConstructor(String::class.java, String::class.java)
        .newInstance(klibFile.canonicalPath, null)

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
