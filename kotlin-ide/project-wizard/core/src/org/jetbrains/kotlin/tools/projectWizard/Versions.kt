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
    val KTOR = version("1.2.6")

    object ANDROID {
        val ANDROIDX_CORE_KTX = version("1.1.0")
        val ANDROIDX_APPCOMPAT = version("1.1.0")
        val ANDROIDX_CONSTRAINTLAYOUT = version("1.1.3")
    }

    object KOTLINX {
        val KOTLINX_HTML = version("0.6.12")
    }

    object JS_WRAPPERS {
        val KOTLIN_REACT = version("16.9.0-pre.89-kotlin-1.3.60")
        val KOTLIN_REACT_DOM = KOTLIN_REACT
        val KOTLIN_STYLED = KOTLIN_REACT
    }

    object NPM {
        val REACT = version("16.12.0")
        val REACT_DOM = REACT
        val REACT_IS = REACT

        val STYLED_COMPONENTS = version("5.0.0")
        val INLINE_STYLE_PREFIXER = version("5.1.0")
    }

    object GRADLE_PLUGINS {
        val ANDROID = version("3.5.2")
    }
}

private fun version(version: String) = Version.fromString(version)