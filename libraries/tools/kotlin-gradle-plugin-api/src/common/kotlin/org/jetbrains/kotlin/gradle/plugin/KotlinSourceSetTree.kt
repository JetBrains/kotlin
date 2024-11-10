/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

/**
 * Represents a tree of shared [KotlinSourceSets][KotlinSourceSet].
 *
 * When applying any [KotlinHierarchyTemplate], such as calling `kotlin.applyDefaultHierarchyTemplate()`) in multiplatform projects,
 * the descriptor is applied to individual [compilations][KotlinCompilation], creating multiple trees of shared
 * [KotlinSourceSets][KotlinSourceSet].
 *
 * The [name] is used as a suffix for each shared [KotlinSourceSet] within the target hierarchy.
 * For example, for the `common` group within the `main` tree, the shared [KotlinSourceSet] name is `commonMain`.
 *
 * The default hierarchy template will create two well-known `SourceSetTrees`:
 * - The `main` tree with `commonMain` as its root [KotlinSourceSet]
 * - The `test` tree with `commonTest` as its root [KotlinSourceSet]
 *
 * For example, for the following snippet:
 * ```kotlin
 * kotlin {
 *     applyDefaultHierarchyTemplate()
 *     jvm()
 *     iosX64()
 *     iosArm64()
 * }
 * ```
 *
 * The plugin creates two [KotlinSourceSetTrees][KotlinSourceSetTree] called "main" and "test":
 * ```
 *                    "main"                               "test"
 *
 *                  commonMain                           commonTest
 *                      |                                    |
 *                      |                                    |
 *           +----------+----------+              +----------+----------+
 *           |                     |              |                     |
 *          ...                  jvmMain         ...                  jvmTest
 *           |                                    |
 *           |                                    |
 *         iosMain                              iosTest
 *           |                                    |
 *      +----+-----+                         +----+-----+
 *      |          |                         |          |
 * iosX64Main   iosArm64Main            iosX64Test   iosArm64Test
 * ```
 *
 * Typically, the name of the [KotlinCompilation] matches the name of the `SourceSetTree`. For example,
 * the "main" tree, rooted at "commonMain", contains all "main" compilations of the project
 * targets.
 * Similarly, the "test" tree, rooted at "commonTest", includes all "test" compilations of the project targets.
 *
 * There are some exceptions where the user can't choose the name of the compilations directly. For example, on Android.
 * In this case, the name of the `KotlinSourceSetTree` can be configured outside the target hierarchy DSL.
 *
 * @param name the name of this `KotlinSourceSetTree`
 */
class KotlinSourceSetTree(val name: String) {

    /**
     * @suppress
     */
    override fun toString(): String = name

    /**
     * @suppress
     */
    override fun equals(other: Any?): Boolean {
        if (other !is KotlinSourceSetTree) return false
        return this.name == other.name
    }

    /**
     * @suppress
     */
    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Constants and static properties for the [KotlinSourceSetTree].
     */
    companion object {
        /**
         * The `main` [KotlinSourceSetTree]. Typically, the root [KotlinSourceSet] has the name: `commonMain`.
         */
        val main: KotlinSourceSetTree = KotlinSourceSetTree("main")

        /**
         * The `test` [KotlinSourceSetTree]. Typically, the root [KotlinSourceSet] has the name: `commonTest`.
         */
        val test: KotlinSourceSetTree = KotlinSourceSetTree("test")

        /**
         * A special pre-defined [KotlinSourceSetTree].
         *
         * It can be used to introduce a new tree with `commonUnitTest` as the root [KotlinSourceSet].
         * For example, it's useful
         * for organizing Android [KotlinCompilations][KotlinCompilation]/[KotlinSourceSets][KotlinSourceSet] for unit tests.
         */
        val unitTest: KotlinSourceSetTree = KotlinSourceSetTree("unitTest")


        /**
         * A special pre-defined [KotlinSourceSetTree].
         *
         * It can be used to introduce a new tree with `commonInstrumentedTest` as the root [KotlinSourceSet].
         * For example,
         * it's useful for organizing Android-instrumented [KotlinCompilations][KotlinCompilation]/[KotlinSourceSets][KotlinSourceSet].
         */
        val instrumentedTest: KotlinSourceSetTree = KotlinSourceSetTree("instrumentedTest")


        /**
         * A special pre-defined [KotlinSourceSetTree].
         *
         * It can be used to introduce a new tree with `commonIntegrationTest` as the root [KotlinSourceSet].
         */
        val integrationTest: KotlinSourceSetTree = KotlinSourceSetTree("integrationTest")

    }
}
