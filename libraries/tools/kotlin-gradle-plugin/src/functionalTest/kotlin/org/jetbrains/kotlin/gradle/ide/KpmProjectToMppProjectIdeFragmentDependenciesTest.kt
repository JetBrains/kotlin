/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")
@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.plugin.ide.IdeLocalSourceFragmentDependency
import org.jetbrains.kotlin.gradle.plugin.ide.InternalIdeApi
import org.jetbrains.kotlin.gradle.kpm.jvm
import kotlin.test.Test
import kotlin.test.assertEquals

class KpmProjectToMppProjectIdeFragmentDependenciesTest : AbstractIdeFragmentDependenciesTest() {

    @Test
    fun `sample - with jvm and native targets`() {
        val mpp = createMultiplatformProject("mpp") {
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

        val kpm = createKpmProject("kpm") {
            mainAndTest {
                jvm
                val iosX64 = fragments.create("iosX64", org.jetbrains.kotlin.gradle.kpm.KotlinIosX64Variant::class.java)
                val iosArm64 = fragments.create("iosArm64", org.jetbrains.kotlin.gradle.kpm.KotlinIosArm64Variant::class.java)
                val linuxX64 = fragments.create("linuxX64", org.jetbrains.kotlin.gradle.kpm.KotlinLinuxX64Variant::class.java)

                val ios = fragments.create("ios")
                val native = fragments.create("native")

                ios.refines(common)
                native.refines(common)
                iosX64.refines(ios)
                iosArm64.refines(ios)
                ios.refines(native)
                linuxX64.refines(native)
            }
        }

        resolveIdeDependenciesSet(kpm.main.common)
        resolveIdeDependenciesSet(kpm.main.fragments.getByName("ios"))
        resolveIdeDependenciesSet(kpm.main.fragments.getByName("iosX64"))
        resolveIdeDependenciesSet(kpm.main.fragments.getByName("iosArm64"))
        resolveIdeDependenciesSet(kpm.main.fragments.getByName("linuxX64"))
        resolveIdeDependenciesSet(kpm.main.fragments.getByName("jvm"))

        /* TODO */
        runCatching {
            assertEquals(
                setOf(IdeLocalSourceFragmentDependency(mpp.project, "main", "common")),
                resolveIdeDependenciesSet(kpm.main.common)
            )
        }
    }
}
