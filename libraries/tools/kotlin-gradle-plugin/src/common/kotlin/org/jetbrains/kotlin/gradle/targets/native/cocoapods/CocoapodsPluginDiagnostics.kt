/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory

@InternalKotlinGradlePluginApi // used in integration tests
object CocoapodsPluginDiagnostics {

    object DeprecatedPropertiesUsed : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(usedDeprecatedProperties: List<String>) = build(
            """
            |Properties 
            |    ${usedDeprecatedProperties.joinToString(separator = "\n|    ")}
            |are not supported and will be ignored since CocoaPods plugin generates all required properties automatically.
            """.trimMargin()
        )
    }

    object LinkOnlyUsedWithStaticFramework : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(podName: String) = build(
            """
                 Dependency on '$podName' with option 'linkOnly=true' is unused for building static frameworks.
                 When using static linkage you will need to provide all dependencies for linking the framework into a final application.
            """.trimIndent()
        )
    }

    object UnsupportedOs : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
                Kotlin CocoaPods Plugin is fully supported on MacOS machines only. Gradle tasks that can not run on non-mac hosts will be skipped.
            """.trimIndent()
        )
    }

    object UseLibrariesUsed : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke() = build("'useLibraries' mode is removed")
    }

    object InteropBindingSelfDependency : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(podName: String) = build("Pod '$podName' has an interop-binding dependency on itself")
    }

    object InteropBindingUnknownDependency : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(podName: String, dependencyName: String) = build("Couldn't find declaration of pod '$dependencyName' (interop-binding dependency of pod '${podName}')")
    }

}