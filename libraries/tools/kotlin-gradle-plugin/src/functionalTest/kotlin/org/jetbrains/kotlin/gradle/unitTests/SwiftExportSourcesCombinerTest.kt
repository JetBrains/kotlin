/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportSourceVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTargetModules
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftPackageSourceLanguage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.combineSwiftExportSources
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.combineSwiftExportModules
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.swiftPackageDestinationCondition
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SwiftExportSourcesCombinerTest {

    @Test
    fun `swift conditions of every apple target`() {
        assertEquals(
            mapOf(
                "ios_arm64" to "os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)",
                "ios_simulator_arm64" to "os(iOS) && targetEnvironment(simulator) && arch(arm64)",
                "ios_x64" to "os(iOS) && targetEnvironment(simulator) && arch(x86_64)",
                "macos_arm64" to "os(macOS) && arch(arm64)",
                "macos_x64" to "os(macOS) && arch(x86_64)",
                "tvos_arm64" to "os(tvOS) && !targetEnvironment(simulator) && arch(arm64)",
                "tvos_simulator_arm64" to "os(tvOS) && targetEnvironment(simulator) && arch(arm64)",
                "tvos_x64" to "os(tvOS) && targetEnvironment(simulator) && arch(x86_64)",
                "watchos_arm64" to "os(watchOS) && !targetEnvironment(simulator) && arch(arm64_32)",
                "watchos_device_arm64" to "os(watchOS) && !targetEnvironment(simulator) && arch(arm64)",
                "watchos_simulator_arm64" to "os(watchOS) && targetEnvironment(simulator) && arch(arm64)",
                "watchos_x64" to "os(watchOS) && targetEnvironment(simulator) && arch(x86_64)",
            ),
            appleTargets.associate { it.name to it.swiftPackageDestinationCondition(SwiftPackageSourceLanguage.SWIFT) },
        )
    }

    @Test
    fun `c conditions of every apple target`() {
        assertEquals(
            mapOf(
                "ios_arm64" to "__is_target_os(ios) && __is_target_environment(unknown) && __is_target_arch(arm64)",
                "ios_simulator_arm64" to "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(arm64)",
                "ios_x64" to "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(x86_64)",
                "macos_arm64" to "__is_target_os(macos) && __is_target_arch(arm64)",
                "macos_x64" to "__is_target_os(macos) && __is_target_arch(x86_64)",
                "tvos_arm64" to "__is_target_os(tvos) && __is_target_environment(unknown) && __is_target_arch(arm64)",
                "tvos_simulator_arm64" to "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(arm64)",
                "tvos_x64" to "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(x86_64)",
                "watchos_arm64" to "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64_32)",
                "watchos_device_arm64" to "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64)",
                "watchos_simulator_arm64" to "__is_target_os(watchos) && __is_target_environment(simulator) && __is_target_arch(arm64)",
                "watchos_x64" to "__is_target_os(watchos) && __is_target_environment(simulator) && __is_target_arch(x86_64)",
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
            #if __is_target_os(ios) && __is_target_environment(unknown) && __is_target_arch(arm64)
            #include <stdint.h>

            int32_t device();
            #elif __is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64_32)
            #include <stdint.h>

            int32_t watch();
            #endif
            """.trimIndent() + "\n",
            combined,
        )
    }

    @Test
    fun `a source that only some targets have is wrapped in their branch`() {
        val ios = "public func ios() {}\n"
        val combined = combineSwiftExportSources(
            listOf(
                SwiftExportSourceVariant(KonanTarget.IOS_ARM64, ios),
                SwiftExportSourceVariant(KonanTarget.IOS_SIMULATOR_ARM64, ios),
            ),
            SwiftPackageSourceLanguage.SWIFT,
            packageTargets = listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.MACOS_ARM64),
        )

        assertEquals(
            """
            #if (os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)) || (os(iOS) && targetEnvironment(simulator) && arch(arm64))
            public func ios() {}
            #endif
            """.trimIndent() + "\n",
            combined,
        )
    }

    @Test
    fun `the package has every module and every dependency of any target`() {
        val modules = combineSwiftExportModules(perPlatformTargets)

        assertEquals(
            mapOf(
                "Shared" to listOf("Runtime", "IosOnly", "MacosOnly"),
                "Runtime" to emptyList(),
                "IosOnly" to listOf("Runtime"),
                "MacosOnly" to emptyList(),
            ),
            modules.associate { it.module.name to it.module.dependencies },
        )
        assertEquals(
            mapOf(
                "Shared" to setOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.MACOS_ARM64),
                "Runtime" to setOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.MACOS_ARM64),
                "IosOnly" to setOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64),
                "MacosOnly" to setOf(KonanTarget.MACOS_ARM64),
            ),
            modules.associate { it.module.name to it.variants.keys },
        )
    }

    @Test
    fun `a dependency that every target of a platform has is limited to that platform`() {
        assertEquals(
            mapOf(
                // Only the dependencies of Shared need a condition: IosOnly always depends on Runtime where it's exported.
                "Shared" to mapOf("IosOnly" to setOf(Family.IOS), "MacosOnly" to setOf(Family.OSX)),
                "Runtime" to emptyMap(),
                "IosOnly" to emptyMap(),
                "MacosOnly" to emptyMap(),
            ),
            combineSwiftExportModules(perPlatformTargets).associate { it.module.name to it.dependencyPlatforms },
        )
    }

    @Test
    fun `a dependency that only some targets of a platform have is not limited`() {
        val modules = combineSwiftExportModules(
            listOf(
                SwiftExportTargetModules(
                    "iosArm64", KonanTarget.IOS_ARM64,
                    listOf(bridged("Shared", "ios", listOf("DeviceOnly")), swiftOnly("DeviceOnly", "ios")),
                ),
                SwiftExportTargetModules("iosSimulatorArm64", KonanTarget.IOS_SIMULATOR_ARM64, listOf(bridged("Shared", "sim"))),
                SwiftExportTargetModules("macosArm64", KonanTarget.MACOS_ARM64, listOf(bridged("Shared", "macos"))),
            )
        )

        assertEquals(emptyMap(), modules.single { it.module.name == "Shared" }.dependencyPlatforms)
    }

    private val perPlatformTargets = listOf(
        SwiftExportTargetModules(
            "iosArm64", KonanTarget.IOS_ARM64,
            listOf(
                bridged("Shared", "ios", listOf("Runtime", "IosOnly")),
                swiftOnly("Runtime", "ios"),
                bridged("IosOnly", "ios", listOf("Runtime")),
            ),
        ),
        SwiftExportTargetModules(
            "iosSimulatorArm64", KonanTarget.IOS_SIMULATOR_ARM64,
            listOf(
                bridged("Shared", "sim", listOf("Runtime", "IosOnly")),
                swiftOnly("Runtime", "sim"),
                bridged("IosOnly", "sim", listOf("Runtime")),
            ),
        ),
        SwiftExportTargetModules(
            "macosArm64", KonanTarget.MACOS_ARM64,
            listOf(
                bridged("Shared", "macos", listOf("Runtime", "MacosOnly")),
                swiftOnly("Runtime", "macos"),
                swiftOnly("MacosOnly", "macos"),
            ),
        ),
    )

    // Every Apple target, the deprecated ones included. watchosArm32 is the one Swift Export doesn't support:
    // it has no AppleArchitecture.
    private val appleTargets = KonanTarget.predefinedTargets.values.filter { it.family.isAppleFamily } - KonanTarget.WATCHOS_ARM32

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
