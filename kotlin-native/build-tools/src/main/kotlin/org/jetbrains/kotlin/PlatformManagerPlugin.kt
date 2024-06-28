/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import kotlinBuildProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.*

open class PlatformManagerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val konanDataDir = project.kotlinBuildProperties.getOrNull("konan.data.dir") as String?
        val platformManager = PlatformManager(buildDistribution(project.project(":kotlin-native").projectDir.absolutePath, konanDataDir), false)
        project.extensions.add("platformManager", platformManager)
    }
}
