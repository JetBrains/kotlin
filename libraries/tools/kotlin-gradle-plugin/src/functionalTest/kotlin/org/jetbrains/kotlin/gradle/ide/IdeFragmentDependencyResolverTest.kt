/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.ide

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.enableHierarchicalStructureByDefault
import org.jetbrains.kotlin.gradle.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.ide.IdeLocalSourceFragmentDependency
import org.jetbrains.kotlin.gradle.plugin.ide.IdeFragmentDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.InternalIdeApi
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeFragmentDependencyResolverTest {

    @Test
    fun `test kpm project to project dependency`() {
        /* Setup project p1 & p2 */
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val p1 = ProjectBuilder.builder().withName("p1").withParent(project).build() as ProjectInternal
        val p2 = ProjectBuilder.builder().withName("p2").withParent(project).build() as ProjectInternal

        p1.applyKpmPlugin()
        p2.applyKpmPlugin()

        p2.pm20Extension.apply {
            mainAndTest {
                setupKotlinVariantsForTest()
            }
        }

        p1.pm20Extension.apply {
            mainAndTest {
                setupKotlinVariantsForTest()
                dependencies {
                    api(project(":p2"))
                }
            }
        }

        /* Test resolver */
        val resolver = IdeFragmentDependencyResolver.create(p1)

        assertEquals(
            setOf(p2.newSourceDependency("main", "common")),
            resolver.resolveDependencies("main", "common").toSet()
        )

        assertEquals(
            setOf(
                p2.newSourceDependency("main", "common"),
                p2.newSourceDependency("main", "native")
            ),
            resolver.resolveDependencies("main", "native").toSet()
        )

        assertEquals(
            setOf(
                p2.newSourceDependency("main", "common"),
                p2.newSourceDependency("main", "native"),
                p2.newSourceDependency("main", "ios")
            ),
            resolver.resolveDependencies("main", "ios").toSet()
        )

        assertEquals(
            setOf(
                p2.newSourceDependency("main", "common"),
                p2.newSourceDependency("main", "native"),
                p2.newSourceDependency("main", "linuxX64")
            ),
            resolver.resolveDependencies("main", "linuxX64").toSet()
        )
    }

    @Test
    fun `test kpm project to mpp project dependency`() {
        /* Setup project p1 & p2 */
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val p1 = ProjectBuilder.builder().withName("p1").withParent(project).build() as ProjectInternal
        val p2 = ProjectBuilder.builder().withName("p2").withParent(project).build() as ProjectInternal

        project.enableHierarchicalStructureByDefault()
        p2.enableHierarchicalStructureByDefault()
        p2.enableHierarchicalStructureByDefault()

        p1.applyKpmPlugin()
        p2.applyMultiplatformPlugin()
        p2.multiplatformExtension.setupKotlinTargetsForTest()
        p1.pm20Extension.apply {
            mainAndTest {
                setupKotlinVariantsForTest()
                dependencies {
                    api(project(":p2"))
                }
            }
        }

        project.evaluate()
        p2.evaluate()
        p1.evaluate()

        /* Test resolver */
        val resolver = IdeFragmentDependencyResolver.create(p1)

        resolver.resolveDependencies("main", "common").toSet()
        resolver.resolveDependencies("main", "native").toSet()
        resolver.resolveDependencies("main", "ios").toSet()
        resolver.resolveDependencies("main", "linuxX64").toSet()

        /* TODO Assert non-empty dependencies
        assertEquals(
            setOf(p2.newSourceDependency("main", "common")),
            resolver.resolveDependencies("main", "common").toSet()
        )
         */
    }

    private fun KotlinGradleModule.setupKotlinVariantsForTest() {
        /* Variants */
        jvm
        val iosX64 = fragments.create("iosX64", KotlinIosX64Variant::class.java)
        val iosArm64 = fragments.create("iosArm64", KotlinIosArm64Variant::class.java)
        val linuxX64 = fragments.create("linuxX64", KotlinLinuxX64Variant::class.java)

        /* Shared fragments */
        val ios = fragments.create("ios")
        val native = fragments.create("native")

        ios.refines(common)
        native.refines(common)
        iosX64.refines(ios)
        iosArm64.refines(ios)
        ios.refines(native)
        linuxX64.refines(native)
    }

    private fun KotlinMultiplatformExtension.setupKotlinTargetsForTest() {
        jvm()
        iosX64()
        iosArm64()
        linuxX64()

        val commonMain = sourceSets.getByName("commonMain")
        val iosMain = sourceSets.create("iosMain")
        val nativeMain = sourceSets.create("nativeMain")
        val iosX64Main = sourceSets.getByName("iosX64Main")
        val iosArm64Main = sourceSets.getByName("iosArm64Main")
        val linuxX64Main = sourceSets.getByName("linuxX64Main")

        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosMain.dependsOn(nativeMain)
        nativeMain.dependsOn(commonMain)
        linuxX64Main.dependsOn(nativeMain)
        nativeMain.dependsOn(commonMain)
    }

    private fun Project.newSourceDependency(
        kotlinModuleName: String, kotlinFragmentName: String
    ) = IdeLocalSourceFragmentDependency(
        buildId = project.currentBuildId(),
        projectPath = path,
        projectName = name,
        kotlinModuleName = kotlinModuleName,
        kotlinFragmentName = kotlinFragmentName
    )
}
