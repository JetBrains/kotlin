/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime

class NativeImageCompilerRunner(javaHome: String = System.getProperty("java.home")) : CompilerRunner(javaHome) {
    override val executable: String by lazy {
        val launcher = if (isWindows) "kotlinc-native-image.bat" else "kotlinc-native-image.sh"
        ForTestCompileRuntime.kotlinNativeImageDistForTests().resolve("bin").resolve(launcher).absolutePath
    }
}
