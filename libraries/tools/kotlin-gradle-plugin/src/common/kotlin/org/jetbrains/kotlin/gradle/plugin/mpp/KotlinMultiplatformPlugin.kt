/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

class KotlinMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaBasePlugin::class.java)
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}
