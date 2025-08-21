/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Interface representing a common plugin for resolving NPM dependencies within a Gradle project.
 *
 * This plugin provides a mechanism to resolve dependencies managed by Node.js and NPM,
 * enabling consistent configuration and processing of NPM-related tasks across projects.
 * It ensures integration with npm dependency management capabilities,
 * including dependency installation and task execution wiring.
 *
 * Implementing this interface allows projects to configure and apply NPM dependency
 * resolution strategies specific to their needs.
 */
interface CommonNpmResolverPlugin : Plugin<Project>