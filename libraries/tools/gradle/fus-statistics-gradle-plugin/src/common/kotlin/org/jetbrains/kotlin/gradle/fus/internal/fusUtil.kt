/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project

private const val FUS_STATISTICS_PATH = "kotlin.session.logger.root.path"

fun Project.getFusDirectory() = providers.gradleProperty(FUS_STATISTICS_PATH).orNull ?: gradle.gradleUserHomeDir.path