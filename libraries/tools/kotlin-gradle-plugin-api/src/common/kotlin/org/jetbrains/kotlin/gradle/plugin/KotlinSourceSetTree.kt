/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

/**
 * When applying any [KotlinHierarchyTemplate] (e.g. by calling `applyDefaultHierarchyTemplate()`), the descriptor will
 * be applied on individual compilations, creating multiple trees of shared SourceSets.
 *
 * The name of given shared source set within the target hierarchy will be built by
 * `lowerCamelCase(nameOfGroup, nameOfSourceSetTree)`
 * So for the 'common' group within the 'main' tree the shared SourceSet name will be 'commonMain'
 *
 * The most default case will create two well known SourceSetTrees:
 * - The `main` tree with `commonMain` as its root SourceSet
 * - The `test` tree with `commonTest` as its root SourceSet
 *
 * e.g.
 * ```kotlin
 * kotlin {
 *     applyDefaultHierarchyTemplate()
 *     jvm()
 *     iosX64()
 *     iosArm64()
 * }
 * ```
 *
 * will create two SourceSetTrees: "main" and "test"
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
 *
 * Usually, the name of the compilation correlates to the name of the SourceSetTree:
 * As seen in the previous example, the "main" tree under the "commonMain" root contains all 'main' compilations of the projects
 * targets. The "test" tree under the "commonTest" root contains all 'test' compilations of the projects targets.
 *
 * There are some exceptions, where the name of the compilations cannot be chosen by the user directly (Android)
 * In this case, the name of the SourceSet can be configured outside the target hierarchy DSL.
 */
class KotlinSourceSetTree(val name: String) {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other !is KotlinSourceSetTree) return false
        return this.name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        /**
         * The 'main' SourceSetTree. Typically, with 'commonMain' as the root SourceSet
         */
        val main: KotlinSourceSetTree = KotlinSourceSetTree("main")

        /**
         * The 'test' SourceSetTree. Typically, with 'commonTest' as the root SourceSet
         */
        val test: KotlinSourceSetTree = KotlinSourceSetTree("test")

        /**
         * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonUnitTest' as the root SourceSet
         * e.g. relevant for organising Android unitTest compilations/SourceSets
         */
        val unitTest: KotlinSourceSetTree = KotlinSourceSetTree("unitTest")


        /**
         * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonInstrumentedTest' as the root SourceSet
         * e.g. relevant for organising Android instrumented compilations/SourceSets
         */
        val instrumentedTest: KotlinSourceSetTree = KotlinSourceSetTree("instrumentedTest")


        /**
         * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonIntegrationTest' as root SourceSEt
         */
        val integrationTest: KotlinSourceSetTree = KotlinSourceSetTree("integrationTest")

    }
}
