/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration

interface KotlinDependencyConfigurations {
    val apiConfiguration: Configuration
    val implementationConfiguration: Configuration
    val compileOnlyConfiguration: Configuration
    val runtimeOnlyConfiguration: Configuration
}
