/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.utils

import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportLogger
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig

internal fun logConfigIssues(modules: Set<InputModule>, logger: SwiftExportLogger) {
    modules.forEach { module ->
        val config = module.config
        if (config.bridgeModuleName == SwiftModuleConfig.DEFAULT_BRIDGE_MODULE_NAME) {
            logger.report(
                SwiftExportLogger.Severity.Warning,
                "Bridging header is not set. Using `${SwiftModuleConfig.DEFAULT_BRIDGE_MODULE_NAME}` instead"
            )
        }
        if (config.targetPackageFqName == null) {
            logger.report(
                SwiftExportLogger.Severity.Warning,
                "No name for `${SwiftModuleConfig.ROOT_PACKAGE}` and will be ignored"
            )
        }
    }
}
