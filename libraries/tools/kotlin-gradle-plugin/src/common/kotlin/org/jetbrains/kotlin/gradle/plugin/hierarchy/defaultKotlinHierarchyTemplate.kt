/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate.Templates
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget



/**
 * Set's up a 'natural'/'default' hierarchy within [KotlinTarget]'s, inside the project.
 *
 * #### Example 1
 *
 * ```kotlin
 * kotlin {
 *     applyDefaultHierarchyTemplate() // <- position of this call is not relevant!
 *
 *     iosX64()
 *     iosArm64()
 *     linuxX64()
 *     linuxArm64()
 * }
 * ```
 *
 * Will create the following SourceSets:
 * `[iosMain, iosTest, appleMain, appleTest, linuxMain, linuxTest, nativeMain, nativeTest]
 *
 *
 * Hierarchy:
 * ```
 *                                                                     common
 *                                                                        |
 *                                                      +-----------------+-------------------+
 *                                                      |                                     |
 *
 *                                                    native                                 ...
 *
 *                                                     |
 *                                                     |
 *                                                     |
 *         +----------------------+--------------------+-----------------------+
 *         |                      |                    |                       |
 *
 *       apple                  linux                mingw              androidNative
 *
 *         |
 *  +-----------+------------+------------+
 *  |           |            |            |
 *
 * macos       ios         tvos        watchos
 * ```
 */
val Templates.default get() = defaultKotlinHierarchyTemplate

private val defaultKotlinHierarchyTemplate = KotlinHierarchyTemplate {
    /* natural hierarchy is only applied to default 'main'/'test' compilations (by default) */
    withSourceSetTree(KotlinSourceSetTree.main, KotlinSourceSetTree.test)

    common {
        /* All compilations shall be added to the common group by default */
        withCompilations { true }

        group("native") {
            withNative()

            group("apple") {
                withApple()

                group("ios") {
                    withIos()
                }

                group("tvos") {
                    withTvos()
                }

                group("watchos") {
                    withWatchos()
                }

                group("macos") {
                    withMacos()
                }
            }

            group("linux") {
                withLinux()
            }

            group("mingw") {
                withMingw()
            }

            group("androidNative") {
                withAndroidNative()
            }
        }
    }
}
