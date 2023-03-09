/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

interface KotlinTargetHierarchy {

    data class ModuleName(val name: String) {
        override fun toString(): String = name

        companion object {
            val main = ModuleName("main")
            val test = ModuleName("test")
            val unitTest = ModuleName("unitTest")
            val integrationTest = ModuleName("integrationTest")
            val instrumentedTest = ModuleName("instrumentedTest")
        }
    }
}
