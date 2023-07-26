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
         * Default hierarchy for Kotlin Multiplatform projects.
         * This hierarchy template will be applied to projects 'by default' if the project does not opt-out
         * and is compatible.
         *
         * Incompatible projects include:
         *  - projects that setup custom dependsOn edges
         *  - projects that define custom target names matching one of the shared source sets (e.g. linuxX64("linux"))
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
        this@extend.impl.layout(this)
        describe()
    }
}

/*
INTERNAL API
 */

@InternalKotlinGradlePluginApi
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.Root.applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
    template.impl.layout(this)
}

internal val KotlinHierarchyTemplate.impl
    get() = when (this) {
        is KotlinHierarchyTemplateImpl -> this
    }

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal class KotlinHierarchyTemplateImpl(
    private val describe: KotlinHierarchyBuilder.Root.() -> Unit,
) : KotlinHierarchyTemplate {
    fun layout(builder: KotlinHierarchyBuilder.Root) {
        describe(builder)
    }

    override fun hashCode(): Int {
        return describe.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KotlinHierarchyTemplateImpl) return false
        return this.describe == other.describe
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
