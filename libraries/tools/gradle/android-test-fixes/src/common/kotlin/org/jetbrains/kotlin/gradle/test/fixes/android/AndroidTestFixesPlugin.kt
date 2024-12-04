/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test.fixes.android

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.test.fixes.android.fixes.applyDebugKeystoreFix

class AndroidTestFixesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val testFixesProperties = TestFixesProperties(target)
        target.applyDebugKeystoreFix(testFixesProperties)
    }
}