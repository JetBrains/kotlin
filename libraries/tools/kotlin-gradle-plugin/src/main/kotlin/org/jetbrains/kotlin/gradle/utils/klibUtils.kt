/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project

internal fun Project.klibModuleName(baseName: String = project.name): String =
    if (group.toString().isNotEmpty()) "$group:$baseName" else baseName