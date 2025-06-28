/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jebrains.kotlin.backend.native.driver.utilities

import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.backend.common.phaser.getIrDumper
import org.jetbrains.kotlin.backend.common.phaser.getIrValidator
import org.jebrains.kotlin.backend.native.PhaseContext

/**
 * IR dump and verify actions.
 */
fun <Data, Context : PhaseContext> getDefaultIrActions(): Set<Action<Data, Context>> = setOfNotNull(
        getIrDumper(),
        getIrValidator(checkTypes = true)
)
