/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.wsm

//import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.ALL
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.OLDER

//@Serializable
class K2JSDceArgumentsWsmGenerated : CommonToolArgumentsWsmGenerated() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    var outputDirectory: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var declarationsToKeep: Array<String>? = null
        set(value) {
            
            field = value
        }

    var printReachabilityInfo = false
        set(value) {
            
            field = value
        }

    var devMode = false
        set(value) {
            
            field = value
        }

    var devModeOverwritingStrategy: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }


}

object DevModeOverwritingStrategies {
    const val OLDER = "older"
    const val ALL = "all"
}