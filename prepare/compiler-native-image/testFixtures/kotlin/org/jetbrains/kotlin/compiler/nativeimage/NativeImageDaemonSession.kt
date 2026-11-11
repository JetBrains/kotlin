/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.daemon.common.CompilerId

class NativeImageDaemonSession : DaemonSession() {
    private val daemonExecutable = ForTestCompileRuntime.kotlinNativeImageDistForTests().resolve("bin")
        .resolve(if (isWindows) "kotlin-daemon-native-image.exe" else "kotlin-daemon-native-image")
    override val compilerId = CompilerId.makeCompilerId(daemonExecutable)

    override val executable: String = daemonExecutable.absolutePath
}
