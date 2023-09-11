/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

/**
 * A DSL to apply hierarchy templates in a Kotlin project.
 */
interface KotlinHierarchyDsl {

    /**
     * Applies a given [template] to the project.
     *
     * *Examples:*
     *
     * - Manually apply the default hierarchy
     * (See `KotlinMultiplatformExtension.applyDefaultHierarchyTemplate`):
     * ```kotlin
     * kotlin {
     *     applyHierarchyTemplate(KotlinHierarchyTemplate.default)
     *     iosX64()
     *     iosArm64()
     *     iosSimulatorArm64()
     *     linuxX64()
     *     // ...
     * }
     * ```
     */
    fun applyHierarchyTemplate(template: KotlinHierarchyTemplate)

    /**
     * Similar to [applyHierarchyTemplate], but allows extension of the provided template.
     *
     * *Examples:*
     *
     * - Add custom groups (Experimental) to additionally share code between Linux and Apple (unixLike):
     *
     * ```kotlin
     * kotlin {
     *     applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
     *         group("native") { // <- we can re-declare already existing groups and connect children to it!
     *             group("unixLike") {
     *                 withLinux()
     *                 withApple()
     *             }
     *         }
     *     }
     * }
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    fun applyHierarchyTemplate(template: KotlinHierarchyTemplate, extension: KotlinHierarchyBuilder.Root.() -> Unit)

    /**
     * Allows creating a fully custom hierarchy (no defaults applied).
     *
     * **Note: ** Using the custom hierarchy requires setting the edges to 'commonMain' and 'commonTest' SourceSets by
     * using the `common` group.
     *
     * *Examples:*
     *
     * - Share code between iOS and JVM targets:
     * ```kotlin
     * applyHierarchyTemplate {
     *     common {
     *         withJvm()
     *         group("ios") {
     *             withIos()
     *         }
     *     }
     * }
     * ```
     *
     * This configuration creates two [KotlinSourceSetTree] using the 'common' and 'ios' groups,
     * applied on the "test" and "main" compilations.
     * When the following targets are specified:
     * - jvm()
     * - iosX64()
     * - iosArm64()
     * ```
     *                    "main"                               "test"
     *                  commonMain                           commonTest
     *                      |                                    |
     *                      |                                    |
     *           +----------+----------+              +----------+----------+
     *           |                     |              |                     |
     *         iosMain               jvmMain        iosTest               jvmTest
     *           |                                    |
     *      +----+-----+                         +----+-----+
     *      |          |                         |          |
     * iosX64Main   iosArm64Main            iosX64Test   iosArm64Test
     * ```
     *
     * - Create a 'diamond structure'
     * ```kotlin
     * applyHierarchyTemplate {
     *     common {
     *         group("ios") {
     *             withIos()
     *         }
     *
     *         group("frontend") {
     *             withJvm()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *
     *         group("apple") {
     *             withMacos()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *     }
     * }
     * ```
     *
     * In this case, the _group_ "ios" can be created with 'group("ios")' and later referenced with the same construction to build
     * the tree.
     * Apply the descriptor from the example to the following targets:
     * - iosX64()
     * - iosArm64()
     * - macosX64()
     * - jvm()
     *
     * To create the following 'main' KotlinSourceSetTree:
     *
     * ```
     *                      commonMain
     *                           |
     *              +------------+----------+
     *              |                       |
     *          frontendMain            appleMain
     *              |                        |
     *    +---------+------------+-----------+----------+
     *    |                      |                      |
     * jvmMain                iosMain               macosX64Main
     *                           |
     *                           |
     *                      +----+----+
     *                      |         |
     *                iosX64Main   iosArm64Main
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    fun applyHierarchyTemplate(template: KotlinHierarchyBuilder.Root.() -> Unit)
}