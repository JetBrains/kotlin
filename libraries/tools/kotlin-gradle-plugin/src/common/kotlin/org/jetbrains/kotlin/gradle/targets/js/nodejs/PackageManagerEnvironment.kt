/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel
import java.io.Serializable

@ObsoleteNpmDependencyModel
interface PackageManagerEnvironment : Serializable