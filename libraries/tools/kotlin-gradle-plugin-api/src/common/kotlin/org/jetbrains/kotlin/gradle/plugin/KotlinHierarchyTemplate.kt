/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi


/**
 * Defines a hierarchy of [KotlinSourceSets][KotlinSourceSet] in a multiplatform project.
 *
 * By default, the [default template][KotlinHierarchyTemplate.default] is applied in multiplatform projects (with some exceptions,
 * check [Templates.default] for more details).
 *
 * To create a custom template, use the `kotlin.applyHierarchyTemplate { }` method.
 */
sealed interface KotlinHierarchyTemplate {

    /**
     * Pre-defined [KotlinSourceSets][KotlinSourceSet] hierarchy templates.
     */
    companion object Templates {
        /**
         * The default [KotlinSourceSets][KotlinSourceSet] hierarchy template for Kotlin Multiplatform projects.
         *
         * This hierarchy template is applied to projects 'by default' if they are compatible
         * unless the project opts out via the Gradle property `kotlin.mpp.applyDefaultHierarchyTemplate=false`,
         * or defines a custom [KotlinHierarchyTemplate] (for example, via `kotlin.applyHierarchyTemplate { .. }`).
         *
         * Incompatible projects include:
         *  - Projects that configure custom [KotlinSourceSet.dependsOn] edges.
         *  - Projects that define custom target names matching one of the shared source sets. For example, `kotlin.linuxX64("linux")`.
         *
         * The default hierarchy is:
         * ```text
         *                                    common
         *                                      │
         *                         ┌────────────┴─────────────────┬───────────┐
         *                         │                              │           │
         *                       native                          web         ...
         *                         │                              │
         *             ┌───────┬───┴───┬───────────┐          ┌───┴───┐
         *             │       │       │           │          │       │
         *           apple   linux   mingw   androidNative    js    wasmJs
         *             │
         *   ┌──────┬──┴──┬────────┐
         *   │      │     │        │
         * macos   ios   tvos   watchos
         * ```
         */
        val default get() = defaultKotlinHierarchyTemplate
    }
}

//region EXPERIMENTAL API

/**
 * Creates a new [KotlinHierarchyTemplate] using inputs provided via the [describe] definition.
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate(
    describe: KotlinHierarchyBuilder.Root.() -> Unit,
): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplateImpl(describe)
}

/**
 * Creates a new [KotlinHierarchyTemplate] by extending the existing one and using inputs provided via the [describe]
 * definition.
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate.extend(describe: KotlinHierarchyBuilder.Root.() -> Unit): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplate {
        this@extend.impl.layout(this)
        describe()
    }
}

//endregion

//region INTERNAL API

/**
 * @suppress
 */
@InternalKotlinGradlePluginApi
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.Root.applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
    template.impl.layout(this)
}

/**
 * @suppress
 */
internal val KotlinHierarchyTemplate.impl
    get() = when (this) {
        is KotlinHierarchyTemplateImpl -> this
    }

/**
 * @suppress
 */
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

/**
 * @suppress
 * The default hierarchy template implementation
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

        group("web") {
            withJs()
            withWasmJs()
        }
    }
}

//endregion
