/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

sealed class ProjectVariant {
    data object JavaOnly : ProjectVariant()
    data object AndroidOnly : ProjectVariant()
    data class Kmp(val variants: List<String>) : ProjectVariant() {
        constructor(string: String): this(listOf(string))

        operator fun plus(that: Kmp): Kmp = Kmp(this.variants + that.variants)

        val withAndroid get() = "android" in variants
        val withJvm get() = "jvm" in variants

        fun isCommonMainDependableOn(dependency: ProjectVariant): Boolean {
            if (dependency !is Kmp) return false
            return (variants - dependency.variants).isEmpty()
        }

        override fun toString(): String = "kmp_${variants.joinToString("_")}"
    }

    companion object {
        val native = Kmp("native")
        val jvm = Kmp("jvm")
        val android = Kmp("android")
        val javaOnly = JavaOnly
        val androidOnly = AndroidOnly
    }
}