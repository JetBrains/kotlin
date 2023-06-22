/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

@Deprecated("Use KotlinSourceSetTree instead")
interface KotlinTargetHierarchy {

    @Deprecated("Use KotlinSourceSetTree instead", replaceWith = ReplaceWith("KotlinSourceSetTree"))
    class SourceSetTree(val name: String)  {
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
            val main = SourceSetTree("main")
            val test = SourceSetTree("test")
            val unitTest = SourceSetTree("unitTest")
            val instrumentedTest = SourceSetTree("instrumentedTest")
            val integrationTest = SourceSetTree("integrationTest")
        }
    }
}
