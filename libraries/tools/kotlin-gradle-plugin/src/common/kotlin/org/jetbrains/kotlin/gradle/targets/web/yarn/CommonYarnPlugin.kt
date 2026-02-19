/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Represents a common plugin interface for integrating Yarn within a Gradle Project.
 *
 * This plugin is designed for managing Yarn-specific configurations, tasks, and behaviors
 * to facilitate the integration of the Yarn package manager into the build process.
 *
 * By implementing this interface, the plugin can provide support for configuring Yarn environments,
 * installing dependencies, enforcing lockfile management, and handling resolutions for specific package versions.
 */
interface CommonYarnPlugin : Plugin<Project>