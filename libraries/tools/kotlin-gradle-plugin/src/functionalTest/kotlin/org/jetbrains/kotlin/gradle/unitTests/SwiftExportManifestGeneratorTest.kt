/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SPMManifestGenerator
import kotlin.test.Test
import java.io.File
import kotlin.test.assertEquals

class SwiftExportManifestGeneratorTest {

    @Test
    fun `test swift export SPM manifest generation`() {
        val swiftApiModule = "Shared"

        val manifest = SPMManifestGenerator.generateManifest(
            swiftApiModule,
            "SharedLibrary",
            "KotlinRuntime",
            sharedModulesFixture(swiftApiModule)
        )

        val manifestGold = sharedLibraryManifestGold()

        assertEquals(manifestGold, manifest)
    }

    @Test
    fun `test swift export SPM manifest complicated generation`() {
        val swiftApiModule = "Shared"

        val manifest = SPMManifestGenerator.generateManifest(
            swiftApiModule,
            "SharedLibrary",
            "KotlinRuntime",
            complicatedModulesFixture(swiftApiModule)
        )

        val manifestGold = complicatedManifestGold()

        assertEquals(manifestGold, manifest)
    }

    @Test
    fun `test swift export SPM empty manifest generation`() {
        val swiftApiModule = "Shared"

        val manifest = SPMManifestGenerator.generateManifest(
            swiftApiModule,
            "SharedLibrary",
            "KotlinRuntime",
            emptyModulesFixture()
        )

        val manifestGold = emptyManifestGold()

        assertEquals(manifestGold, manifest)
    }

    @Test
    fun `test generated manifest has no trailing commas before closing brackets`() {
        val manifest = SPMManifestGenerator.generateManifest(
            "Shared",
            "SharedLibrary",
            "KotlinRuntime",
            sharedModulesFixture("Shared")
        )

        val expectedManifest = sharedLibraryManifestGold()

        assertEquals(expectedManifest, manifest)
    }

    @Test
    fun `test single module manifest has no trailing commas`() {
        val manifest = SPMManifestGenerator.generateManifest(
            "SingleModule",
            "SingleLibrary",
            "KotlinRuntime",
            singleModuleFixture()
        )

        val expectedManifest = singleModuleManifestGold()

        assertEquals(expectedManifest, manifest)
    }

    private fun sharedModulesFixture(swiftApiModule: String): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File(""),
                File(""),
                File("")
            ),
            "SharedBridge",
            swiftApiModule,
            listOf("Dependency")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "Dependency",
            emptyList()
        )
    )

    private fun complicatedModulesFixture(swiftApiModule: String): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File(""),
                File(""),
                File("")
            ),
            "SharedBridge",
            swiftApiModule,
            listOf("Dependency1", "Dependency2")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "Dependency1",
            emptyList()
        ),
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "Dependency2",
            listOf("Dependency3")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "Dependency3",
            listOf("Dependency4")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "Dependency4",
            emptyList()
        ),
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File(""),
                File(""),
                File("")
            ),
            "Dependency5Bridge",
            "Dependency5",
            listOf("Dependency4")
        ),
    )

    private fun emptyModulesFixture(): List<GradleSwiftExportModule> = emptyList()

    private fun singleModuleFixture(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.SwiftOnly(
            File(""),
            "SingleModule",
            emptyList()
        )
    )

    private fun complicatedManifestGold() = """
        // swift-tools-version: 5.9

        import PackageDescription
        let package = Package(
            name: "Shared",
            products: [
                .library(
                    name: "SharedLibrary",
                    targets: ["Shared", "Dependency1", "Dependency2", "Dependency3", "Dependency4", "Dependency5"]
                )
            ],
            targets: [
                .target(
                    name: "Shared",
                    dependencies: ["Dependency1", "Dependency2", "SharedBridge", "KotlinRuntime"]
                ),
                .target(
                    name: "SharedBridge"
                ),
                .target(
                    name: "Dependency1",
                    dependencies: ["KotlinRuntime"]
                ),
                .target(
                    name: "Dependency2",
                    dependencies: ["Dependency3", "KotlinRuntime"]
                ),
                .target(
                    name: "Dependency3",
                    dependencies: ["Dependency4", "KotlinRuntime"]
                ),
                .target(
                    name: "Dependency4",
                    dependencies: ["KotlinRuntime"]
                ),
                .target(
                    name: "Dependency5",
                    dependencies: ["Dependency4", "Dependency5Bridge", "KotlinRuntime"]
                ),
                .target(
                    name: "Dependency5Bridge"
                ),
                .target(
                    name: "KotlinRuntime"
                )
            ]
        )
    """.trimIndent() + "\n"

    private fun emptyManifestGold() = """
        // swift-tools-version: 5.9

        import PackageDescription
        let package = Package(
            name: "Shared",
            products: [
                .library(
                    name: "SharedLibrary",
                    targets: []
                )
            ],
            targets: [
                .target(
                    name: "KotlinRuntime"
                )
            ]
        )
    """.trimIndent() + "\n"

    private fun singleModuleManifestGold() = """
        // swift-tools-version: 5.9

        import PackageDescription
        let package = Package(
            name: "SingleModule",
            products: [
                .library(
                    name: "SingleLibrary",
                    targets: ["SingleModule"]
                )
            ],
            targets: [
                .target(
                    name: "SingleModule",
                    dependencies: ["KotlinRuntime"]
                ),
                .target(
                    name: "KotlinRuntime"
                )
            ]
        )
    """.trimIndent() + "\n"

    private fun sharedLibraryManifestGold() = """
        // swift-tools-version: 5.9

        import PackageDescription
        let package = Package(
            name: "Shared",
            products: [
                .library(
                    name: "SharedLibrary",
                    targets: ["Shared", "Dependency"]
                )
            ],
            targets: [
                .target(
                    name: "Shared",
                    dependencies: ["Dependency", "SharedBridge", "KotlinRuntime"]
                ),
                .target(
                    name: "SharedBridge"
                ),
                .target(
                    name: "Dependency",
                    dependencies: ["KotlinRuntime"]
                ),
                .target(
                    name: "KotlinRuntime"
                )
            ]
        )
    """.trimIndent() + "\n"
}
