/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.utils.isProperSubsetOf
import org.jetbrains.kotlin.commonizer.utils.isSubsetOf

internal fun selectInputTargets(inputTargets: Set<CommonizerTarget>, outputTarget: SharedCommonizerTarget): Set<CommonizerTarget> {
    val subsetInputTargets = inputTargets
        .filter { inputTarget -> inputTarget != outputTarget && inputTarget.allLeaves() isSubsetOf outputTarget.allLeaves() }
        .sortedBy { it.allLeaves().size }

    val disjointSubsetInputTargets = subsetInputTargets
        .filter { inputTarget ->
            subsetInputTargets.none { potentialSuperSet -> inputTarget.allLeaves() isProperSubsetOf potentialSuperSet.allLeaves() }
        }

    return outputTarget.allLeaves().fold(setOf()) { selectedInputTargets, outputLeafTarget ->
        if (outputLeafTarget in selectedInputTargets.allLeaves()) return@fold selectedInputTargets
        selectedInputTargets + (disjointSubsetInputTargets.firstOrNull { inputTarget -> outputLeafTarget in inputTarget.allLeaves() }
            ?: failedSelectingInputTargets(inputTargets, outputTarget))
    }
}

private fun failedSelectingInputTargets(inputTargets: Set<CommonizerTarget>, outputTarget: SharedCommonizerTarget): Nothing {
    throw IllegalArgumentException(
        "Failed selecting input targets for $outputTarget\n" +
                "inputTargets=$inputTargets\n" +
                "missing leaf targets: ${outputTarget.allLeaves() - inputTargets.allLeaves()}"
    )
}
