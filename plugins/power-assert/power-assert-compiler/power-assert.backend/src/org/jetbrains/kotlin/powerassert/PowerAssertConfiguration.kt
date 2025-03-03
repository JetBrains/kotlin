/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.name.FqName

class PowerAssertConfiguration(
    private val configuration: CompilerConfiguration,
    val functions: Set<FqName>,
) {
    val constTracker: EvaluatedConstTracker? get() = configuration[CommonConfigurationKeys.EVALUATED_CONST_TRACKER]
    val messageCollector: MessageCollector get() = configuration.messageCollector
}
