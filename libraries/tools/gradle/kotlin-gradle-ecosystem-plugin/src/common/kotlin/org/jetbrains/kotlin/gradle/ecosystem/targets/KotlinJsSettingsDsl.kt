/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.targets

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.ecosystem.npm.JsNpmDependencyManagementDsl
import javax.inject.Inject

/**
 * `js { ... }` block of [KotlinEcosystemExtension].
 *
 * Configures build-wide settings of the Kotlin/JS targets.
 *
 * @since 2.5.20
 */
abstract class KotlinJsSettingsDsl @Inject internal constructor(
    objects: ObjectFactory,
) {

    /**
     * Configures how the npm dependencies of the Kotlin/JS targets of the build are installed.
     *
     * @see JsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    val npmDependencyManagement: JsNpmDependencyManagementDsl =
        objects.newInstance(JsNpmDependencyManagementDsl::class.java)

    /**
     * Configures how the npm dependencies of the Kotlin/JS targets of the build are installed.
     *
     * @see JsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    fun npmDependencyManagement(configure: JsNpmDependencyManagementDsl.() -> Unit) {
        npmDependencyManagement.configure()
    }

    /**
     * [Action] based version of [npmDependencyManagement] above.
     *
     * @see JsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    fun npmDependencyManagement(configure: Action<JsNpmDependencyManagementDsl>) {
        npmDependencyManagement {
            configure.execute(this)
        }
    }
}
