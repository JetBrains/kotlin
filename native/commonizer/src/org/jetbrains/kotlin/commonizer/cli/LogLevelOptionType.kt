/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.CommonizerLogLevel

internal object LogLevelOptionType : OptionType<CommonizerLogLevel>("log-level", "{quiet, info}", false) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<CommonizerLogLevel> {
        return when (rawValue.lowercase().trim()) {
            "quiet" -> Option(this, CommonizerLogLevel.Quiet)
            "info" -> Option(this, CommonizerLogLevel.Info)
            else -> Option(this, CommonizerLogLevel.Quiet)
        }
    }
}
