/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*

internal class LoggingResultsConsumer(
    private val outputCommonizerTarget: SharedCommonizerTarget
) : ResultsConsumer {
    override fun targetConsumed(parameters: CommonizerParameters, target: CommonizerTarget) {
        parameters.logger?.progress("Written libraries for ${outputCommonizerTarget.prettyName(target)}")
    }
}
