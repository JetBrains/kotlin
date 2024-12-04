/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test.fixes.android.fixes

import com.android.build.api.dsl.*
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.test.fixes.android.TestFixesProperties

/**
 * AGP 7+ creates a keystore that is not compatible with lover versions of AGP,
 * but could consume keystores created by them.
 *
 * With this fix 'debug.keystore' could be checked in into the repo and shared
 * between test executions.
 */
internal fun Project.applyDebugKeystoreFix(
    testFixesProperties: TestFixesProperties
) {
    plugins.withId("com.android.application", fix<ApplicationExtension>(testFixesProperties))
    plugins.withId("com.android.library", fix<LibraryExtension>(testFixesProperties))
    plugins.withId("com.android.test", fix<TestExtension>(testFixesProperties))
}

private inline fun <reified AndroidExtension : CommonExtension<*, *, *, *>> Project.fix(
    testFixesProperties: TestFixesProperties
): Action<Plugin<*>> = Action {
    extensions.configure<AndroidExtension> {
        logger.info("Reconfiguring Android debug keystore")
        buildTypes.named("debug") { buildType ->
            val signingConfig = when (buildType) {
                is ApplicationBuildType -> buildType.signingConfig
                is LibraryBuildType -> buildType.signingConfig
                is TestBuildType -> buildType.signingConfig
                else -> null
            }

            signingConfig?.storeFile = file(testFixesProperties.androidDebugKeystoreLocation)
        }
    }
}
