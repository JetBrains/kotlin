/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.target.ConfigurablesWithEmulator
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * [Executor] that runs the process in an emulator (e.g. qemu).
 *
 * @param configurables [Configurables] for emulated target
 */
class EmulatorExecutor(
        private val configurables: ConfigurablesWithEmulator,
) : Executor {
    private val hostExecutor: Executor = HostExecutor()

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val absoluteTargetSysRoot = configurables.absoluteTargetSysRoot
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile

        return hostExecutor.execute(request.copying {
            this.executableAbsolutePath = configurables.absoluteEmulatorExecutable
            this.workingDirectory = workingDirectory
            this.args.add(0, request.executableAbsolutePath)
            // TODO: Move these to konan.properties when when it will be possible
            //       to represent absolute path there.
            this.args.addAll(0, listOf("-L", absoluteTargetSysRoot))
        })
    }
}
