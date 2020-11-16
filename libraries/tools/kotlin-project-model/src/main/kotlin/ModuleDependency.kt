/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

sealed class ModuleDependency(open val moduleOrigin: ModuleOrigin)

data class ExternalModuleDependency(override val moduleOrigin: ExternalOrigin) : ModuleDependency(moduleOrigin) {
    override fun toString() = "external dependency ${moduleOrigin.dependencyIdParts.joinToString(":")}"
}

data class LocalModuleDependency(override val moduleOrigin: LocalBuild, val moduleName: String) :
    ModuleDependency(moduleOrigin) {
    override fun toString() = "local module $moduleName (build ${moduleOrigin.buildId})"
}