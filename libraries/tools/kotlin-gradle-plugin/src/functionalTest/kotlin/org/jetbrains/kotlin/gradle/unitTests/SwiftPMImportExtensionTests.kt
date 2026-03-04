/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
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
                `package`("https://foo.bar/package1", "1.2.3", listOf("product"))
                `package`("https://foo.bar/package2.git", "1.2.3", listOf("product"))
                localPackage(project.layout.projectDirectory.dir("."), listOf("product"))
                localPackage(project.layout.projectDirectory.dir("../relativePackage"), listOf("product"))
                localPackage(project.layout.projectDirectory.dir("package"), listOf("product"))
                localPackage(project.layout.projectDirectory.dir("sub/subpackage"), listOf("product"))
            }
        }.locateOrRegisterSwiftPMDependenciesExtension().spmDependencies.map {
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

}