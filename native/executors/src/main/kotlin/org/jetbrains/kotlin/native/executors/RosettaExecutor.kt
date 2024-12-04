/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * [Executor] that runs the process using Apple's Rosetta 2.
 *
 * @param configurables [Configurables] for target
 */
class RosettaExecutor(
    private val configurables: AppleConfigurables,
) : Executor {
    companion object {
        /**
         * Returns `true` if running via Rosetta 2 can be made available for given [configurables].
         *
         * This does not check that Rosetta 2 is installed.
         */
        fun availableFor(configurables: AppleConfigurables): Boolean {
            return HostManager.host is KonanTarget.MACOS_ARM64 && configurables.target is KonanTarget.MACOS_X64
        }

        /**
         * Return `true` if Rosetta 2 is installed.
         *
         * @param [hostExecutor] executor in which to run the check. By default [HostExecutor].
         */
        fun checkIsInstalled(hostExecutor: Executor = HostExecutor()): Boolean {
            if (HostManager.host !is KonanTarget.MACOS_ARM64) {
                return false
            }
            return hostExecutor.execute(ExecuteRequest("/usr/bin/arch").apply {
                this.args.addAll(listOf("-x86_64", "/usr/bin/true"))
            }).exitCode == 0
        }
    }

    private val hostExecutor: Executor = HostExecutor()

    private val target by configurables::target

    init {
        require(availableFor(configurables)) {
            "$target cannot be run under Rosetta 2 from host ${HostManager.host}"
        }
        require(checkIsInstalled(hostExecutor)) {
            "Rosetta 2 is not installed on host ${HostManager.host}"
        }
    }

    // When Rosetta 2 is installed, all x64 binaries are automatically run through it.
    override fun execute(request: ExecuteRequest): ExecuteResponse = hostExecutor.execute(request)
}