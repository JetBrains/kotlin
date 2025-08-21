/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Defines a root plugin interface for managing common Node.js configurations in a Gradle project.
 *
 * CommonNodeJsRootPlugin is responsible for integrating Node.js runtime or tooling setups at the root project level.
 * It coordinates with environment specifications and other plugins to ensure that the Node.js execution environment
 * is correctly configured and available across projects in a multi-project build.
 *
 * This interface is intended to be implemented by plugins that require Node.js functionality for tasks such as building
 * JavaScript applications, managing npm dependencies, or running Node.js-based tools in a unified environment.
 */
interface CommonNodeJsRootPlugin : Plugin<Project>