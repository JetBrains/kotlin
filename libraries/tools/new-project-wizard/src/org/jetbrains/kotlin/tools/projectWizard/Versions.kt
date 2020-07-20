/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

@Suppress("ClassName", "SpellCheckingInspection")
object Versions {
    val KOTLIN = version("1.4-M1") // used as fallback version
    val GRADLE = version("6.3")
    val KTOR: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("1.3.2-$kotlinVersion") }

    object ANDROID {
        val ANDROIDX_CORE_KTX = version("1.2.0")
        val ANDROIDX_APPCOMPAT = version("1.1.0")
        val ANDROIDX_CONSTRAINTLAYOUT = version("1.1.3")
    }

    object KOTLINX {
        val KOTLINX_HTML: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("0.7.1-$kotlinVersion") }
        val KOTLINX_NODEJS: Version = version("0.0.3")
    }

    object JS_WRAPPERS {
        val KOTLIN_REACT: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("16.13.1-pre.109-kotlin-$kotlinVersion") }
        val KOTLIN_REACT_DOM = KOTLIN_REACT
        val KOTLIN_STYLED: (kotlinVersion: Version) -> Version = { kotlinVersion -> version("1.0.0-pre.109-kotlin-$kotlinVersion") }
    }

    object GRADLE_PLUGINS {
        val ANDROID = version("3.5.2")
    }
}

private fun version(version: String) = Version.fromString(version)