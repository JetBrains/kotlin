/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.AbstractKpmExtensionTest

abstract class AbstractIdeaKpmFragmentsContentTest : AbstractKpmExtensionTest() {
    protected open fun doSetupProject() {
        with(kotlin) {
            mainAndTest {
                fragments.create("jvm", org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmJvmVariant::class.java)
                val linuxX64Variant =
                    fragments.create("linuxX64", org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmLinuxX64Variant::class.java)
                val iosX64Variant =
                    fragments.create("iosX64", org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosX64Variant::class.java)
                val iosArm64Variant =
                    fragments.create("iosArm64", org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosArm64Variant::class.java)
                val iosCommon = fragments.create("iosCommon")
                val nativeCommon = fragments.create("nativeCommon")

                linuxX64Variant.refines(nativeCommon)

                nativeCommon.refines(common)
                iosCommon.refines(nativeCommon)
                iosX64Variant.refines(iosCommon)
                iosArm64Variant.refines(iosCommon)
            }
        }
    }

}