/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.ALL
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.OLDER

@Serializable
class K2JSDceArguments : CommonToolArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @GradleDeprecatedOption(
        message = "Use task 'destinationDirectory' to configure output directory",
        level = DeprecationLevel.WARNING,
        removeAfter = "1.9.0"
    )
    var outputDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var declarationsToKeep: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var printReachabilityInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    var devMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    var devModeOverwritingStrategy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    override fun copyOf(): Freezable = copyK2JSDceArguments(this, K2JSDceArguments())
}

object DevModeOverwritingStrategies {
    const val OLDER = "older"
    const val ALL = "all"
}