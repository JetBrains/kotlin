/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.isolated

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

/**
 * `true` when the Isolated Projects compatible NPM resolution must be used
 * instead of the legacy root-project based one.
 *
 * This is the only place where the condition is evaluated.
 *
 * @see PropertiesProvider.isolatedNpmResolution
 */
internal val Project.isIsolatedNpmResolutionEnabled: Boolean
    get() = PropertiesProvider(this).isolatedNpmResolution.get()
