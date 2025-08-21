/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.Project

/**
 * Get Native "proto" distribution.
 *
 * Only [konan.properties][NativeDistribution.konanProperties] is available inside it.
 */
val Project.nativeProtoDistribution: NativeDistribution
    get() = NativeDistribution(project(":kotlin-native").layout.projectDirectory)