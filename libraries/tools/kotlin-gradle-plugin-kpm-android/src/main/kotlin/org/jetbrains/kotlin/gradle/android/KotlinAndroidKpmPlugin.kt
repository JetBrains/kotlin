/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinAndroidKpmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.quiet("Applied Kotlin/Android KPM prototype. This is just a proof of concept implementation.")
    }
}
