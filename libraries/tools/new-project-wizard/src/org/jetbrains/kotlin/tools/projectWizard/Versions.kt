/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

@Suppress("ClassName", "SpellCheckingInspection")
object Versions {
    val KOTLIN = version("1.4.10") // used as fallback version
    val GRADLE = version("6.6.1")
    val KTOR = version("1.4.0")
    val JUNIT = version("4.13")
    val JUNIT5 = version("5.6.0")
    val JETBRAINS_COMPOSE = version("0.1.0-dev106")

    val KOTLIN_VERSION_FOR_COMPOSE = version("1.4.0")

    object ANDROID {
        val ANDROID_MATERIAL = version("1.2.1")
        val ANDROIDX_APPCOMPAT = version("1.2.0")
        val ANDROIDX_CONSTRAINTLAYOUT = version("2.0.2")
        val ANDROIDX_KTX = version("1.3.1")
    }

    object KOTLINX {
        val KOTLINX_HTML = version("0.7.2")
        val KOTLINX_NODEJS: Version = version("0.0.7")
    }

    object JS_WRAPPERS {
        val KOTLIN_REACT = wrapperVersion("16.13.1")
        val KOTLIN_REACT_DOM = KOTLIN_REACT
        val KOTLIN_STYLED = wrapperVersion("1.0.0")
        val KOTLIN_REACT_ROUTER_DOM = wrapperVersion("5.1.2")
        val KOTLIN_REDUX = wrapperVersion("4.0.0")
        val KOTLIN_REACT_REDUX = wrapperVersion("5.0.7")

        private fun wrapperVersion(version: String): Version =
            version("$version-pre.113-kotlin-1.4.0")
    }

    object GRADLE_PLUGINS {
        val ANDROID = version("4.0.1")
    }

    object MAVEN_PLUGINS {
        val SUREFIRE = version("2.22.2")
        val FAILSAFE = SUREFIRE
    }
}

private fun version(version: String) = Version.fromString(version)