/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

/**
 * @suppress
 */
@Deprecated("Scheduled for removal in Kotlin 2.3. Use KotlinSourceSetTree instead", level = DeprecationLevel.ERROR)
interface KotlinTargetHierarchy {

    /**
     * @suppress
     */
    @Deprecated(
        "Scheduled for removal in Kotlin 2.3. Use KotlinSourceSetTree instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("KotlinSourceSetTree")
    )
    class SourceSetTree(val name: String)  {
        override fun toString(): String = name

        override fun equals(other: Any?): Boolean {
            if (other !is KotlinSourceSetTree) return false
            return this.name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        @Deprecated(
            "Scheduled for removal in Kotlin 2.3. Use KotlinSourceSetTree instead",
            level = DeprecationLevel.ERROR,
            replaceWith = ReplaceWith("KotlinSourceSetTree")
        )
        @Suppress("DEPRECATION_ERROR")
        companion object {
            val main = SourceSetTree("main")
            val test = SourceSetTree("test")
            val unitTest = SourceSetTree("unitTest")
            val instrumentedTest = SourceSetTree("instrumentedTest")
            val integrationTest = SourceSetTree("integrationTest")
        }
    }
}
