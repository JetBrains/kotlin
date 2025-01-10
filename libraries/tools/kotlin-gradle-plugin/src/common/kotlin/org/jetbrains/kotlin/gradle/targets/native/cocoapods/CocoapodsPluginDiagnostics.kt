/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import java.net.URI

@InternalKotlinGradlePluginApi // used in integration tests
object CocoapodsPluginDiagnostics {

    object DeprecatedPropertiesUsed : ToolingDiagnosticFactory(WARNING) {
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

    object LinkOnlyUsedWithStaticFramework : ToolingDiagnosticFactory(WARNING) {
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

    object UnsupportedOs : ToolingDiagnosticFactory(WARNING) {
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

    object InteropBindingSelfDependency : ToolingDiagnosticFactory(ERROR) {
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

    object InteropBindingUnknownDependency : ToolingDiagnosticFactory(ERROR) {
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

    object EmbedAndSignUsedWithPodDependencies : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke() = build {
            title("Incompatible 'embedAndSign' Task with Pod Dependencies")
                .description {
                    """
                    'embedAndSign' task can't be used in a project with dependencies to pods.
                    
                    To temporarily suppress this error, put the following in your gradle.properties:
                        
                        ${PropertiesProvider.PropertyNames.KOTLIN_APPLE_ALLOW_EMBED_AND_SIGN_WITH_COCOAPODS}=true
                    
                    Please note that this property is deprecated and it will be removed in the upcoming releases
                    """.trimIndent()
                }
                .solution {
                    "Migrate to CocoaPods for integration into Xcode"
                }
                .documentationLink(URI("https://kotl.in/vc2iq3")) { url ->
                    "Please migrate to CocoaPods for integration into Xcode: $url"
                }
        }
    }
}