/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import java.net.URI

internal object CocoapodsPluginDiagnostics {

    object DeprecatedPropertiesUsed : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(usedDeprecatedProperties: List<String>) = build {
            title("Deprecated Properties Used")
                .description {
                    """
                    |Properties 
                    |    ${usedDeprecatedProperties.joinToString(separator = "\n|    ")}
                    |are not supported and will be ignored since CocoaPods plugin generates all required properties automatically.
                    """.trimMargin()
                }
                .solution {
                    "Remove the deprecated properties from the build script"
                }
        }
    }

    object LinkOnlyUsedWithStaticFramework : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(podName: String) = build {
            title("Link-Only Option Ignored")
                .description {
                    """
                    Dependency on '$podName' with option 'linkOnly=true' is unused for building static frameworks.
                    When using static linkage you will need to provide all dependencies for linking the framework into a final application.
                    """.trimIndent()
                }
                .solution {
                    "Remove the 'linkOnly=true' option from the dependency declaration"
                }
        }
    }

    object UnsupportedOs : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("Unsupported Operating System")
                .description {
                    "Kotlin CocoaPods Plugin is fully supported on MacOS machines only. Gradle tasks that can not run on non-mac hosts will be skipped."
                }
                .solution {
                    "Run the build on a MacOS machine"
                }
        }
    }

    object InteropBindingSelfDependency : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(podName: String) = build {
            title("Self-Referential Interop-Binding Dependency")
                .description {
                    "Pod '$podName' has an interop-binding dependency on itself"
                }
                .solution {
                    "Remove the interop-binding dependency on itself"
                }
        }
    }

    object InteropBindingUnknownDependency : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(podName: String, dependencyName: String) = build {
            title("Unknown Interop-Binding Dependency")
                .description {
                    "Couldn't find declaration of pod '$dependencyName' (interop-binding dependency of pod '${podName}')"
                }
                .solution {
                    "Add the missing pod declaration to the build script"
                }
        }
    }

    object EmbedAndSignUsedWithPodDependencies : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("Incompatible 'embedAndSign' Task with CocoaPods Dependencies")
                .description {
                    """
                The 'embedAndSign' task cannot be used in projects that have CocoaPods dependencies configured.
                
                This conflict occurs because:
                • The 'embedAndSign' task is designed for manual framework integration
                • CocoaPods manages its own framework integration and dependency resolution
                • Using both simultaneously can lead to duplicate symbols, conflicting build phases, and unpredictable build behavior
                
                Your project currently has CocoaPods dependencies that conflict with the embedAndSign workflow.
                
                To temporarily suppress this error, add the following to your gradle.properties:
                    
                    ${PropertiesProvider.PropertyNames.KOTLIN_APPLE_ALLOW_EMBED_AND_SIGN_WITH_COCOAPODS}=true
                
                ⚠️  WARNING: This property is deprecated and will be removed in future releases. 
                Using this workaround may cause build issues and is not supported.
                """.trimIndent()
                }
                .solutions {
                    listOf(
                        "Remove CocoaPods dependencies and use 'embedAndSignAppleFrameworkForXcode' task for manual framework integration",
                        "Remove 'embedAndSign' task and migrate to CocoaPods for framework integration"
                    )
                }
                .documentationLink(URI("https://kotl.in/vc2iq3")) { url ->
                    "For detailed migration instructions and best practices, see: $url"
                }
        }
    }
}