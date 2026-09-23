/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.konan.target.Configurables

internal val NativeBackendPhaseContext.cpuModel: String
    get() {
        val target = config.target
        val configurables: Configurables = config.platform.configurables
        return configurables.targetCpu ?: run {
            this@cpuModel.diagnosticReporter.report(NativeBackendDiagnostics.LLVM_WARNING, "targetCpu for target $target was not set. Targeting `generic` cpu.")
            "generic"
        }
    }

internal val NativeBackendPhaseContext.cpuFeatures: String
    get() = config.platform.configurables.targetCpuFeatures ?: ""