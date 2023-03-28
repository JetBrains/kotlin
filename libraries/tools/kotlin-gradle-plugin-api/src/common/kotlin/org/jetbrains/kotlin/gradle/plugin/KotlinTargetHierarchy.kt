/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

interface KotlinTargetHierarchy {

    class ModuleName(val name: String) {
        override fun toString(): String = name

        override fun equals(other: Any?): Boolean {
            if (other !is ModuleName) return false
            return this.name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        companion object {
            val main = ModuleName("main")
            val test = ModuleName("test")
            val unitTest = ModuleName("unitTest")
            val integrationTest = ModuleName("integrationTest")
            val instrumentedTest = ModuleName("instrumentedTest")
        }
    }
}
