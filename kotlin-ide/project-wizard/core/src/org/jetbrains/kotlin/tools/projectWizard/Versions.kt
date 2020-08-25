/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

@Suppress("ClassName", "SpellCheckingInspection")
object Versions {
    val KOTLIN = version("1.4.0-rc") // used as fallback version
    val GRADLE = version("6.3")
    val KTOR: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("1.3.2-$kotlinVersion") }
    val JUNIT = version("4.12")

    object ANDROID {
        val ANDROID_MATERIAL = version("1.2.0")
        val ANDROIDX_APPCOMPAT = version("1.2.0")
        val ANDROIDX_CONSTRAINTLAYOUT = version("1.1.3")
    }

    object KOTLINX {
        val KOTLINX_HTML: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("0.7.1-$kotlinVersion") }
        val KOTLINX_NODEJS: Version = version("0.0.4")
    }

    object JS_WRAPPERS {
        val KOTLIN_REACT: (kotlinVersion: Version) -> Version = wrapperVersion("16.13.1")
        val KOTLIN_REACT_DOM = KOTLIN_REACT
        val KOTLIN_STYLED: (kotlinVersion: Version) -> Version = wrapperVersion("1.0.0")
        val KOTLIN_REACT_ROUTER_DOM: (kotlinVersion: Version) -> Version = wrapperVersion("5.1.2")
        val KOTLIN_REDUX: (kotlinVersion: Version) -> Version = wrapperVersion("4.0.0")
        val KOTLIN_REACT_REDUX: (kotlinVersion: Version) -> Version = wrapperVersion("5.0.7")

        private fun wrapperVersion(version: String): (Version) -> Version =
            { kotlinVersion -> version("$version-pre.110-kotlin-$kotlinVersion") }
    }

    object GRADLE_PLUGINS {
        val ANDROID = version("4.0.1")
    }
}

private fun version(version: String) = Version.fromString(version)
