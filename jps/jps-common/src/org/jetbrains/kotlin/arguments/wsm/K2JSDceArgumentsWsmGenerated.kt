/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.wsm
import org.jetbrains.kotlin.cli.common.arguments.Argument

//import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.ALL
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.OLDER

//@Serializable
class K2JSDceArgumentsWsmGenerated : CommonToolArgumentsWsmGenerated() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(
            value = "-output-dir",
            valueDescription = "<path>",
            description = "Output directory."
    )
    var outputDirectory: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
            value = "-keep",
            valueDescription = "<fully.qualified.name[,]>",
            description = "List of fully-qualified names of declarations that shouldn't be eliminated."
    )
    var declarationsToKeep: Array<String>? = null
        set(value) {
            
            field = value
        }

    @Argument(
            value = "-Xprint-reachability-info",
            description = "Print declarations marked as reachable."
    )
    var printReachabilityInfo = false
        set(value) {
            
            field = value
        }

    @Argument(
            value = "-dev-mode",
            description = "Development mode: don't strip out any code, just copy dependencies."
    )
    var devMode = false
        set(value) {
            
            field = value
        }

    @Argument(
        value = "-Xdev-mode-overwriting-strategy",
        valueDescription = "{$OLDER|$ALL}",
        description = "Overwriting strategy when copying dependencies in development mode."
    )
    var devModeOverwritingStrategy: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }


}

object DevModeOverwritingStrategies {
    const val OLDER = "older"
    const val ALL = "all"
}