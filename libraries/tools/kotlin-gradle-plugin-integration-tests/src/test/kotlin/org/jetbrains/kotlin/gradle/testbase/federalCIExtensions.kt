/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJvm
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInWasm

@MustRunOnChangesInGradle
@MustRunOnChangesInNative
@MustRunOnChangesInJvm
@MustRunOnChangesInJs
@MustRunOnChangesInWasm
@MustRunOnChangesInBuildToolsApi
annotation class MustRunOnChangesInAnyCompiler

@MustRunOnChangesInGradle
@MustRunOnChangesInJs
@MustRunOnChangesInBuildToolsApi
annotation class MustRunOnChangesInJsIrCompiler
