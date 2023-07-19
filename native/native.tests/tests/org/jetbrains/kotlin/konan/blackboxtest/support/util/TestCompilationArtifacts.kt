/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import java.io.File

private fun invokeKlibTool(kotlinNativeClassLoader: ClassLoader, klibFile: File, functionName: String, vararg args: Any): String {
    val libraryClass = Class.forName("org.jetbrains.kotlin.cli.klib.Library", true, kotlinNativeClassLoader)
    val entryPoint = libraryClass.declaredMethods.single { it.name == functionName }
    val lib = libraryClass.getDeclaredConstructor(String::class.java, String::class.java, String::class.java)
        .newInstance(klibFile.canonicalPath, null, "host")

    val output = StringBuilder()
    entryPoint.invoke(lib, output, *args)
    return output.toString()

}

internal fun TestCompilationArtifact.KLIB.getContents(kotlinNativeClassLoader: ClassLoader): String {
    return invokeKlibTool(kotlinNativeClassLoader, klibFile, "contents", false)
}

internal fun TestCompilationArtifact.KLIB.getIr(
    kotlinNativeClassLoader: ClassLoader,
    printSignatures: Boolean = false,
): String {
    return invokeKlibTool(kotlinNativeClassLoader, klibFile, "ir", printSignatures)
}
