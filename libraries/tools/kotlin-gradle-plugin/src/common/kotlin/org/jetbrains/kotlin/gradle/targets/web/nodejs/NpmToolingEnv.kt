/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import java.io.File

/**
 * Represents an environment for managing npm-related tooling.
 *
 * @property version The version of the npm tooling environment.
 * @property dir The root directory associated with the npm tooling environment.
 */
open class NpmToolingEnv(
    val version: String,
    val dir: File,
    val explicitDir: Boolean,
)