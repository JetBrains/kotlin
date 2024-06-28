/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import org.jetbrains.kotlin.backend.common.phaser.Action
import org.jetbrains.kotlin.backend.common.phaser.getIrDumper
import org.jetbrains.kotlin.backend.common.phaser.getIrValidator
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext

/**
 * IR dump and verify actions.
 */
internal fun <Data, Context : PhaseContext> getDefaultIrActions(): Set<Action<Data, Context>> = setOfNotNull(
        getIrDumper(),
        getIrValidator(checkTypes = true)
)
