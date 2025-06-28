
package org.jebrains.kotlin.backend.native

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String) =
    this.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)