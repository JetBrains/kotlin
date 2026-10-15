/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl

import org.gradle.api.Project
import org.jetbrains.kotlin.build.d8.D8Extension

val Project.d8KotlinBuild: D8Extension
    get() = extensions.getByName("d8KotlinBuild") as D8Extension

fun Project.d8KotlinBuild(configure: D8Extension.() -> Unit): Unit =
    d8KotlinBuild.configure()
