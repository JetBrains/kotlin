/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact

internal fun TestCompilationArtifact.KLIB.getContents(kotlinNativeClassLoader: ClassLoader): String {
    val libraryClass = Class.forName("org.jetbrains.kotlin.cli.klib.Library", true, kotlinNativeClassLoader)
    val entryPoint = libraryClass.declaredMethods.single { it.name == "contents" }
    val lib = libraryClass.getDeclaredConstructor(String::class.java, String::class.java, String::class.java)
        .newInstance(klibFile.canonicalPath, null, "host")

    val output = StringBuilder()
    entryPoint.invoke(lib, output, false)
    return output.toString()
}
