/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.npm

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl
import javax.inject.Inject

/**
 * `npmDependencyManagement { ... }` block of [org.jetbrains.kotlin.gradle.ecosystem.targets.KotlinWasmJsSettingsDsl].
 *
 * Configures how the npm dependencies of the Kotlin/Wasm JS projects of the build are installed.
 * See [NpmDependencyManagementDsl] for the options shared with Kotlin/JS.
 *
 * @since 2.5.20
 */
@ExperimentalNpmDependencyManagementDsl
abstract class WasmJsNpmDependencyManagementDsl @Inject internal constructor(
    objects: ObjectFactory,
) : NpmDependencyManagementDsl(objects)
