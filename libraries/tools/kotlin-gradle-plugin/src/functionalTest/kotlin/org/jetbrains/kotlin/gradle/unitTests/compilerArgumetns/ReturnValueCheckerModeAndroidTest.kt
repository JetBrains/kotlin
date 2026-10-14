/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setAndroidSdkDirProperty
import kotlin.test.Test
import kotlin.test.assertTrue

class ReturnValueCheckerModeAndroidTest : ReturnValueCheckerTestBase() {
    @Test
    fun `mode is unset by default`() {
        val project = externalAndroidLibraryProject {
            kotlin {
            }
        }
        project.evaluate()
        val args = project.compileArguments("compileAndroidMain")
        assertTrue(
            args.none { it.contains("return-value-checker") },
            "Args should not contain return value checker: $args"
        )
    }

    @Test
    fun `override main compilation mode`() {
        val project = externalAndroidLibraryProject {
            kotlin {
                returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
            }
        }
        project.evaluate()
        val args = project.compileArguments("compileAndroidMain")
        assertTrue(
            args.any { it.contains("return-value-checker=full") },
            "Args should contain return value checker enabled with full mode: $args"
        )
    }

    @Test
    fun `test mode has no effect`() {
        val project = externalAndroidLibraryProject {
            kotlin {
                returnValueCheckerModeForTests.set(ReturnValueCheckerMode.Full)
            }
        }
        project.evaluate()
        val args = project.compileArguments("compileAndroidMain")
        assertTrue(
            args.none { it.contains("return-value-checker") },
            "Args should not contain return value checker: $args"
        )
    }

    private fun externalAndroidLibraryProject(
        androidLibraryConfiguration: KotlinMultiplatformAndroidLibraryTarget.() -> Unit = {},
        configureProject: Project.() -> Unit = {},
    ): ProjectInternal = buildProject {
        setAndroidSdkDirProperty(project)
        plugins.apply("kotlin-multiplatform")
        plugins.apply("com.android.kotlin.multiplatform.library")
        kotlin {
            iosArm64()
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                target.compileSdk = 34
                target.namespace = "org.jetbrains.sample.options"
                target.withJava()
                target.androidLibraryConfiguration()
            }
        }
        configureProject()
    }
}
