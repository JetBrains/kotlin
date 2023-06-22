/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi


sealed interface KotlinHierarchyTemplate {
    companion object Templates {
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
        val default get() = defaultKotlinHierarchyTemplate
    }
}

/*
EXPERIMENTAL API
 */

@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate(
    describe: KotlinHierarchyBuilder.Root.() -> Unit,
): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplateImpl(describe)
}

@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate.extend(describe: KotlinHierarchyBuilder.Root.() -> Unit): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplate {
        this@extend.internal.layout(this)
        describe()
    }
}

/*
INTERNAL API
 */

@InternalKotlinGradlePluginApi
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.Root.applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
    template.internal.layout(this)
}

internal val KotlinHierarchyTemplate.internal
    get() = when (this) {
        is InternalKotlinHierarchyTemplate -> this
    }

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal interface InternalKotlinHierarchyTemplate : KotlinHierarchyTemplate {
    fun layout(builder: KotlinHierarchyBuilder.Root)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal class KotlinHierarchyTemplateImpl(
    private val describe: KotlinHierarchyBuilder.Root.() -> Unit,
) : InternalKotlinHierarchyTemplate {
    override fun layout(builder: KotlinHierarchyBuilder.Root) {
        describe(builder)
    }
}

/*
Default Hierarchy Template Implementation
 */

@OptIn(ExperimentalKotlinGradlePluginApi::class)
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
