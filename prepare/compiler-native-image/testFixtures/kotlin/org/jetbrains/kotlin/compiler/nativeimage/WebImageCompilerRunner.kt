/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime

/**
 * Runs the Kotlin/Wasm compiler compiled into a GraalVM web image (a WebAssembly module
 * executed by Node.js via the generated JavaScript wrapper).
 */
class WebImageCompilerRunner(javaHome: String = System.getProperty("java.home")) : CompilerRunner(javaHome) {
    override val executable: String by lazy {
        val launcher = if (isWindows) "kotlinc-wasm-web-image.bat" else "kotlinc-wasm-web-image.sh"
        ForTestCompileRuntime.kotlinWebImageDistForTests().resolve("bin").resolve(launcher).absolutePath
    }

    // The web image is not a JVM process, so no JVM system properties can be passed to it
    override val jvmArgs: List<String> get() = emptyList()
}
