/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportModuleGraphMismatch
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportSourceVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTargetModules
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftPackageSourceLanguage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.combineSwiftExportSources
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.findSwiftExportModuleGraphMismatch
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.swiftPackageDestinationCondition
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SwiftExportSourcesCombinerTest {

    @Test
    fun `swift conditions of every apple target`() {
        assertEquals(
            mapOf(
                "ios_arm64" to "os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)",
                "ios_simulator_arm64" to "os(iOS) && targetEnvironment(simulator) && arch(arm64)",
                "ios_x64" to "os(iOS) && targetEnvironment(simulator) && arch(x86_64)",
                "macos_arm64" to "os(macOS) && arch(arm64)",
                "tvos_arm64" to "os(tvOS) && !targetEnvironment(simulator) && arch(arm64)",
                "tvos_simulator_arm64" to "os(tvOS) && targetEnvironment(simulator) && arch(arm64)",
                "watchos_arm64" to "os(watchOS) && !targetEnvironment(simulator) && arch(arm64_32)",
                "watchos_device_arm64" to "os(watchOS) && !targetEnvironment(simulator) && arch(arm64)",
                "watchos_simulator_arm64" to "os(watchOS) && targetEnvironment(simulator) && arch(arm64)",
            ),
            appleTargets.associate { it.name to it.swiftPackageDestinationCondition(SwiftPackageSourceLanguage.SWIFT) },
        )
    }

    @Test
    fun `c conditions of every apple target`() {
        assertEquals(
            mapOf(
                "ios_arm64" to "TARGET_OS_IOS && !TARGET_OS_SIMULATOR && !TARGET_OS_MACCATALYST && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "ios_simulator_arm64" to "TARGET_OS_IOS && TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "ios_x64" to "TARGET_OS_IOS && TARGET_OS_SIMULATOR && TARGET_CPU_X86_64",
                "macos_arm64" to "TARGET_OS_OSX && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "tvos_arm64" to "TARGET_OS_TV && !TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "tvos_simulator_arm64" to "TARGET_OS_TV && TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "watchos_arm64" to "TARGET_OS_WATCH && !TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && !TARGET_RT_64_BIT",
                "watchos_device_arm64" to "TARGET_OS_WATCH && !TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                "watchos_simulator_arm64" to "TARGET_OS_WATCH && TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
            ),
            appleTargets.associate { it.name to it.swiftPackageDestinationCondition(SwiftPackageSourceLanguage.C_HEADER) },
        )
    }

    @Test
    fun `a target that is not an apple target has no condition`() {
        assertFailsWith<IllegalArgumentException> {
            KonanTarget.LINUX_X64.swiftPackageDestinationCondition(SwiftPackageSourceLanguage.SWIFT)
        }
    }

    @Test
    fun `a deprecated apple target has no condition`() {
        listOf(KonanTarget.MACOS_X64, KonanTarget.TVOS_X64, KonanTarget.WATCHOS_X64).forEach { target ->
            val failure = assertFailsWith<IllegalArgumentException> {
                target.swiftPackageDestinationCondition(SwiftPackageSourceLanguage.SWIFT)
            }
            assertEquals(
                "Target ${target.name} is deprecated and is not supported in an exported Swift package",
                failure.message,
            )
        }
    }

    @Test
    fun `identical sources are kept as they are`() {
        val source = "import Foundation\n\npublic func foo() {}\n"

        assertEquals(
            source,
            combineSwiftExportSources(
                listOf(
                    SwiftExportSourceVariant(KonanTarget.IOS_ARM64, source),
                    SwiftExportSourceVariant(KonanTarget.IOS_SIMULATOR_ARM64, source),
                    SwiftExportSourceVariant(KonanTarget.MACOS_ARM64, source),
                ),
                SwiftPackageSourceLanguage.SWIFT,
            ),
        )
    }

    @Test
    fun `different swift sources get a branch each`() {
        val combined = combineSwiftExportSources(
            listOf(
                SwiftExportSourceVariant(KonanTarget.IOS_ARM64, "import UIKit\n\npublic func device() {}\n"),
                SwiftExportSourceVariant(KonanTarget.IOS_SIMULATOR_ARM64, "import UIKit\n\npublic func simulator() {}\n"),
                SwiftExportSourceVariant(KonanTarget.MACOS_ARM64, "public func mac() {}"),
            ),
            SwiftPackageSourceLanguage.SWIFT,
        )

        assertEquals(
            """
            #if os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)
            import UIKit

            public func device() {}
            #elseif os(iOS) && targetEnvironment(simulator) && arch(arm64)
            import UIKit

            public func simulator() {}
            #elseif os(macOS) && arch(arm64)
            public func mac() {}
            #else
            #error("This Swift package exported from Kotlin does not support the current destination")
            #endif
            """.trimIndent() + "\n",
            combined,
        )
    }

    @Test
    fun `targets with the same source share a branch`() {
        val ios = "import UIKit\n\npublic func ios() {}\n"
        val combined = combineSwiftExportSources(
            listOf(
                SwiftExportSourceVariant(KonanTarget.IOS_ARM64, ios),
                SwiftExportSourceVariant(KonanTarget.MACOS_ARM64, "public func mac() {}\n"),
                SwiftExportSourceVariant(KonanTarget.IOS_SIMULATOR_ARM64, ios),
            ),
            SwiftPackageSourceLanguage.SWIFT,
        )

        assertEquals(
            """
            #if (os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)) || (os(iOS) && targetEnvironment(simulator) && arch(arm64))
            import UIKit

            public func ios() {}
            #elseif os(macOS) && arch(arm64)
            public func mac() {}
            #else
            #error("This Swift package exported from Kotlin does not support the current destination")
            #endif
            """.trimIndent() + "\n",
            combined,
        )
    }

    @Test
    fun `different headers get a branch each`() {
        val combined = combineSwiftExportSources(
            listOf(
                SwiftExportSourceVariant(KonanTarget.IOS_ARM64, "#include <stdint.h>\n\nint32_t device();\n"),
                SwiftExportSourceVariant(KonanTarget.WATCHOS_ARM64, "#include <stdint.h>\n\nint32_t watch();\n"),
            ),
            SwiftPackageSourceLanguage.C_HEADER,
        )

        assertEquals(
            """
            #include <TargetConditionals.h>

            #if TARGET_OS_IOS && !TARGET_OS_SIMULATOR && !TARGET_OS_MACCATALYST && TARGET_CPU_ARM64 && TARGET_RT_64_BIT
            #include <stdint.h>

            int32_t device();
            #elif TARGET_OS_WATCH && !TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && !TARGET_RT_64_BIT
            #include <stdint.h>

            int32_t watch();
            #else
            #error "This Swift package exported from Kotlin does not support the current destination"
            #endif
            """.trimIndent() + "\n",
            combined,
        )
    }

    @Test
    fun `the same module graph is accepted whatever the file locations and the order`() {
        assertNull(
            findSwiftExportModuleGraphMismatch(
                listOf(
                    SwiftExportTargetModules(
                        "iosArm64", KonanTarget.IOS_ARM64,
                        listOf(bridged("Shared", "ios", listOf("Dependency", "Runtime")), swiftOnly("Dependency", "ios")),
                    ),
                    SwiftExportTargetModules(
                        "macosArm64", KonanTarget.MACOS_ARM64,
                        listOf(swiftOnly("Dependency", "macos"), bridged("Shared", "macos", listOf("Runtime", "Dependency"))),
                    ),
                )
            )
        )
    }

    @Test
    fun `a module missing for one target is a mismatch`() {
        assertEquals(
            SwiftExportModuleGraphMismatch(
                referenceTarget = "iosArm64",
                otherTarget = "macosArm64",
                differences = listOf("OnlyIos: only for iosArm64", "OnlyMacos: only for macosArm64"),
            ),
            findSwiftExportModuleGraphMismatch(
                listOf(
                    SwiftExportTargetModules(
                        "iosArm64", KonanTarget.IOS_ARM64,
                        listOf(bridged("Shared", "ios"), swiftOnly("OnlyIos", "ios")),
                    ),
                    SwiftExportTargetModules(
                        "macosArm64", KonanTarget.MACOS_ARM64,
                        listOf(bridged("Shared", "macos"), swiftOnly("OnlyMacos", "macos")),
                    ),
                )
            ),
        )
    }

    @Test
    fun `different dependencies of a module are a mismatch with the first target that differs`() {
        assertEquals(
            SwiftExportModuleGraphMismatch(
                referenceTarget = "iosArm64",
                otherTarget = "macosArm64",
                differences = listOf("Shared: depends on [A, B] for iosArm64 and on [A] for macosArm64"),
            ),
            findSwiftExportModuleGraphMismatch(
                listOf(
                    SwiftExportTargetModules("iosArm64", KonanTarget.IOS_ARM64, listOf(bridged("Shared", "ios", listOf("A", "B")))),
                    SwiftExportTargetModules(
                        "iosSimulatorArm64",
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        listOf(bridged("Shared", "sim", listOf("A", "B")))
                    ),
                    SwiftExportTargetModules("macosArm64", KonanTarget.MACOS_ARM64, listOf(bridged("Shared", "macos", listOf("A")))),
                )
            ),
        )
    }

    // Every Apple target that is not deprecated.
    private val appleTargets = listOf(
        KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64,
        KonanTarget.MACOS_ARM64,
        KonanTarget.TVOS_ARM64, KonanTarget.TVOS_SIMULATOR_ARM64,
        KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64, KonanTarget.WATCHOS_SIMULATOR_ARM64,
    )

    private fun bridged(name: String, location: String, dependencies: List<String> = emptyList()) =
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(File("$location/$name.swift"), File("$location/$name.kt"), File("$location/$name.h")),
            "SharedBridge_$name",
            name,
            dependencies,
        )

    private fun swiftOnly(name: String, location: String) =
        GradleSwiftExportModule.SwiftOnly(File("$location/$name.swift"), name, emptyList())
}
