/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

data class NpmPackageVersion(val name: String, var version: String) : RequiredKotlinJsDependency {
    override fun createDependency(objectFactory: ObjectFactory, scope: NpmDependency.Scope): Dependency =
        NpmDependency(objectFactory, scope, name, version)
}