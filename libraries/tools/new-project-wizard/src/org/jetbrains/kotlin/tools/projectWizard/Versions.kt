/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

object Versions {
    val KOTLIN = version("1.3.70")

    object GradlePlugins {
        val ANDROID = version("3.5.2")
    }
}

private fun version(version: String) = Version.fromString(version)