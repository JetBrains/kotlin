/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class KT41641AbsentAndroidTarget : MultiplatformExtensionTest() {
    @Test
    fun `test android plugin without android target`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        kotlin.jvm()

        /* Previously failed with 'Collection is empty.' */
        assertNull(project.findAndroidTarget())
        assertSame(kotlin.android(), project.findAndroidTarget())
    }
}
