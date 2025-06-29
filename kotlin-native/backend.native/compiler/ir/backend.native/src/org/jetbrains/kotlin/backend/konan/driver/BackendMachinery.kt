/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jebrains.kotlin.backend.native.BaseNativeConfig
import org.jetbrains.kotlin.backend.common.phaser.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.ConfigChecks
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseContext

interface BackendPhaseContext : PhaseContext, ConfigChecks

internal class BasicBackendPhaseContext(override val config: KonanConfig) : BasicPhaseContext(config.configuration), BackendPhaseContext

internal inline fun <C : PhaseContext> PhaseEngine<C>.startBackendEngine(config: BaseNativeConfig, body: (PhaseEngine<BackendPhaseContext>) -> Unit) {
    this.useContext(BasicBackendPhaseContext(KonanConfig(config)), body)
}