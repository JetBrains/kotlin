/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinMultiplatformPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}
