/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Defines a common Node.js plugin interface for Gradle projects.
 * This interface can be implemented by plugins that provide Node.js runtime or tooling setups
 * in environments where JavaScript (JS) or WebAssembly (Wasm) dependencies are required.
 *
 * CommonNodeJsPlugin works in conjunction with Node.js root plugins and environment specifications
 * to ensure a proper configuration and installation of the necessary Node.js runtime components.
 */
interface CommonNodeJsPlugin : Plugin<Project>