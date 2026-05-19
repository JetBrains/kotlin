/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.CheckCocoaPodsHasNoSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Product
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Repository.Url
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Version.From
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.searchForGradlew
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckCocoaPodsHasNoSwiftPMDependenciesTests {

    @Test
    fun `fails when direct SwiftPM dependencies are present`() {
        val project = buildProject()
        val task = project.registerCheckTask(
            directDeps = setOf(
                Remote(
                    repository = Url("https://github.com/example/Foo.git"),
                    version = From("1.0.0"),
                    products = listOf(Product("Foo")),
                    cinteropClangModules = emptyList(),
                    packageName = "FooPackage",
                    traits = emptySet(),
                )
            )
        )

        val failure = assertFailsWith<IllegalStateException> { task.action() }

        assertEquals(
            """
                You are using CocoaPods integration with SwiftPM dependencies. Please follow the migration guide https://kotl.in/cocoapods-to-swiftpm-migration
                and run the following command to switch your Xcode project to the SwiftPM integration:
                XCODEPROJ_PATH='/path/to/iosApp.xcodeproj' GRADLE_PROJECT_PATH=':' '${searchForGradlew(project.projectDir)}' -p '${project.projectDir}' ':integrateEmbedAndSign' ':integrateLinkagePackage'
                Direct SwiftPM dependencies: FooPackage
            """.trimIndent(),
            failure.message?.trimIndent()
        )
    }

    @Test
    fun `fails when only transitive SwiftPM dependencies are present`() {
        val project = buildProject()
        val identifier = SwiftPMDependencyIdentifier("transitiveDep", true)
        val task = project.registerCheckTask(
            transitive = TransitiveSwiftPMDependencies(
                mapOf(
                    identifier to SwiftPMImportMetadata(
                        konanTargets = setOf("ios_arm64"),
                        iosDeploymentVersion = null,
                        macosDeploymentVersion = null,
                        watchosDeploymentVersion = null,
                        tvosDeploymentVersion = null,
                        isModulesDiscoveryEnabled = true,
                        dependencies = setOf(
                            Remote(
                                repository = Url("https://github.com/example/Bar.git"),
                                version = From("2.0.0"),
                                products = listOf(Product("Bar")),
                                cinteropClangModules = emptyList(),
                                packageName = "BarPackage",
                                traits = emptySet(),
                            )
                        ),
                    )
                )
            ),
        )

        val failure = assertFailsWith<IllegalStateException> { task.action() }

        assertEquals(
            """
                You are using CocoaPods integration with SwiftPM dependencies. Please follow the migration guide https://kotl.in/cocoapods-to-swiftpm-migration
                and run the following command to switch your Xcode project to the SwiftPM integration:
                XCODEPROJ_PATH='/path/to/iosApp.xcodeproj' GRADLE_PROJECT_PATH=':' '${searchForGradlew(project.projectDir)}' -p '${project.projectDir}' ':integrateEmbedAndSign' ':integrateLinkagePackage'
                Transitive SwiftPM dependencies: $identifier: [BarPackage]
            """.trimIndent(),
            failure.message?.trimIndent()
        )
    }

    @Test
    fun `succeeds when there are no SwiftPM dependencies`() {
        val project = buildProject()
        val task = project.registerCheckTask()

        // Must not throw when there are no direct or transitive SwiftPM deps.
        task.action()
    }

    private fun ProjectInternal.registerCheckTask(
        directDeps: Set<SwiftPMDependency> = emptySet(),
        transitive: TransitiveSwiftPMDependencies = TransitiveSwiftPMDependencies(emptyMap()),
        workspacePathValue: String? = null,
    ): CheckCocoaPodsHasNoSwiftPMDependencies {
        val task = tasks.register("checkSwiftPMDependencies", CheckCocoaPodsHasNoSwiftPMDependencies::class.java).get()
        task.directSwiftPMDependencies.set(directDeps)
        task.transitiveSwiftPMDependencies.set(transitive)
        if (workspacePathValue != null) task.workspacePath.set(workspacePathValue)
        task.gradleProjectPath.set(":")
        task.projectPath.set(projectDir)
        task.rootProjectDir.set(rootProject.projectDir)
        return task
    }
}
