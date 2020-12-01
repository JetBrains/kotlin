/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

data class ModuleDependency(val moduleIdentifier: ModuleIdentifier) {
    override fun toString(): String = "dependency $moduleIdentifier"
}

/**
 * TODO other kinds of dependencies: non-Kotlin: cinterop, CocoaPods, NPM dependencies?
 *  support with different moduleIdentifiers? Introduce other kinds of dependencies than ModuleDependency?
 */
