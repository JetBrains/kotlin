/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

interface KotlinHierarchyDsl {

    /**
     * Will apply the given [template] to the project.
     *
     * #### Example: Manually apply the default hierarchy
     * (see KotlinMultiplatformExtension.applyDefaultHierarchyTemplate)
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
     * Similar to [applyHierarchyTemplate], but allows to extend the provided template
     *
     * #### Example: Adding custom groups (Experimental)
     * Let's imagine we would additionally like to share code between linux and apple (unixLike)
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
     * Allows to create a fully custom hierarchy (no defaults applied)
     * Note: Using the custom hierarchy will also require to set the edges to 'commonMain' and 'commonTest' SourceSets by
     * using the `common` group.
     *
     * ####  Example 1:
     * Sharing code between iOS and a jvmTarget:
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
     * Will create two [KotlinSourceSetTree] using the 'common' and 'ios' groups, applied on the "test" and "main" compilations:
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
     * #### Example 2: Creating a 'diamond structure'
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
     * the tree. Applying the descriptor from the example to the following targets:
     * - iosX64()
     * - iosArm64()
     * - macosX64()
     * - jvm()
     *
     * will create the following 'main' KotlinSourceSetTree:
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