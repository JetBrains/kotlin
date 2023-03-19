/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import java.io.Serializable

interface RequiredKotlinJsDependency : Serializable {
    fun createDependency(objectFactory: ObjectFactory, scope: NpmDependency.Scope = NpmDependency.Scope.DEV): Dependency
}