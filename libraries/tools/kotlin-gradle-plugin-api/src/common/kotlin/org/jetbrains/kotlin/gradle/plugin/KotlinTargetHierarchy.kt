/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

@Deprecated("Use KotlinSourceSetTree instead")
interface KotlinTargetHierarchy {

    @Deprecated("Use KotlinSourceSetTree instead", replaceWith = ReplaceWith("KotlinSourceSetTree"))
    class SourceSetTree(override val name: String) : KotlinSourceSetTree {
        override fun toString(): String = name

        override fun equals(other: Any?): Boolean {
            if (other !is KotlinSourceSetTree) return false
            return this.name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        @Suppress("DEPRECATION")
        companion object {
            /**
             * The 'main' SourceSetTree. Typically, with 'commonMain' as the root SourceSet
             */
            val main = SourceSetTree("main")

            /**
             * The 'test' SourceSetTree. Typically, with 'commonTest' as the root SourceSet
             */
            val test = SourceSetTree("test")

            /**
             * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonUnitTest' as the root SourceSet
             * e.g. relevant for organising Android unitTest compilations/SourceSets
             */
            val unitTest = SourceSetTree("unitTest")


            /**
             * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonInstrumentedTest' as the root SourceSet
             * e.g. relevant for organising Android instrumented compilations/SourceSets
             */
            val instrumentedTest = SourceSetTree("instrumentedTest")


            /**
             * Special pre-defined SourceSetTree: Can be used to introduce a new tree with 'commonIntegrationTest' as root SourceSEt
             */
            val integrationTest = SourceSetTree("integrationTest")
        }
    }
}
