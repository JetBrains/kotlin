/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.kpm.*
import org.jetbrains.kotlin.gradle.plugin.ide.InternalIdeApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KpmProjectToKpmProjectIdeFragmentDependenciesTest : AbstractIdeFragmentDependenciesTest() {

    @Test
    fun `sample - with jvm and native targets`() {
        fun KpmExtension.setupFragmentsForTest() = mainAndTest {
            /* Variants */
            jvm
            val iosX64 = fragments.create("iosX64", org.jetbrains.kotlin.gradle.kpm.KotlinIosX64Variant::class.java)
            val iosArm64 = fragments.create("iosArm64", org.jetbrains.kotlin.gradle.kpm.KotlinIosArm64Variant::class.java)
            val linuxX64 = fragments.create("linuxX64", org.jetbrains.kotlin.gradle.kpm.KotlinLinuxX64Variant::class.java)

            /* Shared fragments */
            val ios = fragments.create("ios")
            val native = fragments.create("native")

            /* Refines edges */
            ios.refines(common)
            native.refines(common)
            iosX64.refines(ios)
            iosArm64.refines(ios)
            ios.refines(native)
            linuxX64.refines(native)
        }

        val p1 = createKpmProject("p1") {
            setupFragmentsForTest()
        }

        val p2 = createKpmProject("p2") {
            setupFragmentsForTest()
            mainAndTest {
                dependencies {
                    api(project(":p1"))
                }
            }
        }

        /*
        Assert :p2 to :p1 dependencies for the main module
         */

        assertEquals(
            ideLocalSourceDependenciesSetOf(p1.main.common),
            resolveIdeDependenciesSet(p2.main.common)
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios")
            ),
            resolveIdeDependenciesSet(p2.main.fragments.getByName("ios"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios"),
                p1.main.fragments.getByName("iosX64")
            ),
            resolveIdeDependenciesSet(p2.main.fragments.getByName("iosX64"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios"),
                p1.main.fragments.getByName("iosArm64")
            ),
            resolveIdeDependenciesSet(p2.main.fragments.getByName("iosArm64"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("linuxX64")
            ),
            resolveIdeDependenciesSet(p2.main.fragments.getByName("linuxX64"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("jvm")
            ),
            resolveIdeDependenciesSet(p2.main.fragments.getByName("jvm"))
        )

        /*
        Assert :p2 to :p1 dependencies for the test module
         */

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p2.main.common
            ),
            resolveIdeDependenciesSet(p2.test.common)
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("jvm"),

                p2.main.common,
                p2.main.fragments.getByName("jvm")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("jvm"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),

                p2.main.common,
                p2.main.fragments.getByName("native")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("native"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios"),

                p2.main.common,
                p2.main.fragments.getByName("native"),
                p2.main.fragments.getByName("ios")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("ios"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios"),
                p1.main.fragments.getByName("iosX64"),

                p2.main.common,
                p2.main.fragments.getByName("native"),
                p2.main.fragments.getByName("ios"),
                p2.main.fragments.getByName("iosX64")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("iosX64"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("ios"),
                p1.main.fragments.getByName("iosArm64"),

                p2.main.common,
                p2.main.fragments.getByName("native"),
                p2.main.fragments.getByName("ios"),
                p2.main.fragments.getByName("iosArm64")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("iosArm64"))
        )

        assertEquals(
            ideLocalSourceDependenciesSetOf(
                p1.main.common,
                p1.main.fragments.getByName("native"),
                p1.main.fragments.getByName("linuxX64"),

                p2.main.common,
                p2.main.fragments.getByName("native"),
                p2.main.fragments.getByName("linuxX64")
            ),
            resolveIdeDependenciesSet(p2.test.fragments.getByName("linuxX64"))
        )
    }
}
