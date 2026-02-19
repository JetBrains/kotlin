/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The base interface for all Kotlin Gradle plugins.
 */
interface KotlinBasePlugin : Plugin<Project> {

    /**
     * The current plugin version.
     */
    val pluginVersion: String
}
