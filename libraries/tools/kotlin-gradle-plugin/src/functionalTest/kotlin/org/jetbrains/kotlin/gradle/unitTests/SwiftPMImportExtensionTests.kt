/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.toSwiftPMPlatforms
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftPMImportExtensionTests {

    @Test
    fun `inferred package names`() {
        val rootProject = buildProject()
        val inferredPackageNames = buildProjectWithMPP(
            projectBuilder = {
                withName("kmp_subproject")
                withParent(rootProject)
            }
        ) {
            locateOrRegisterSwiftPMDependenciesExtension().apply {
                swiftPackage("https://foo.bar/package1", "1.2.3", listOf("product"))
                swiftPackage("https://foo.bar/package2.git", "1.2.3", listOf("product"))
                localSwiftPackage(project.layout.projectDirectory.dir("."), listOf("product"))
                localSwiftPackage(project.layout.projectDirectory.dir("../relativePackage"), listOf("product"))
                localSwiftPackage(project.layout.projectDirectory.dir("package"), listOf("product"))
                localSwiftPackage(project.layout.projectDirectory.dir("sub/subpackage"), listOf("product"))
            }
        }.locateOrRegisterSwiftPMDependenciesExtension().swiftPMDependencies.map {
            it.packageName
        }

        assertEquals(
            listOf(
                "package1",
                "package2",
                "kmp_subproject",
                "relativePackage",
                "package",
                "subpackage",
            ).prettyPrinted,
            inferredPackageNames.prettyPrinted,
        )
    }

    @Test
    fun `published konanTargets of removed targets are ignored`() {
        assertEquals(
            setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.watchOS),
            setOf("ios_arm64", "watchos_arm64", "macos_x64", "tvos_x64", "watchos_x64", "watchos_arm32").toSwiftPMPlatforms(),
        )
    }

}
