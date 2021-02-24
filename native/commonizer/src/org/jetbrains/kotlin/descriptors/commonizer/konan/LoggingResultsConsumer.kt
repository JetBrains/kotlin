/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer
import org.jetbrains.kotlin.descriptors.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.prettyName
import org.jetbrains.kotlin.util.Logger

internal class LoggingResultsConsumer(
    private val outputCommonizerTarget: SharedCommonizerTarget, private val logger: Logger
) : ResultsConsumer {
    override fun targetConsumed(target: CommonizerTarget) {
        logger.log("Written libraries for ${outputCommonizerTarget.prettyName(target)}")
    }
}
