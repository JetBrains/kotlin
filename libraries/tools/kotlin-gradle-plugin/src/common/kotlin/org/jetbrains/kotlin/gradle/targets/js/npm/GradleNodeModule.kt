/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.Serializable

/**
 * Fake NodeJS module directory created from Gradle external module
 */
data class GradleNodeModule(val name: String, val version: String, val path: File) : Serializable {
    val semver: SemVer
        get() = SemVer.from(version)

    @get:Synchronized
    val dependencies: Set<NpmDependencyDeclaration> by lazy {
        val pJson = Json.parseToJsonElement(path.resolve("package.json").readText()).jsonObject
        mapOf(
            NpmDependency.Scope.NORMAL to pJson["dependencies"],
            NpmDependency.Scope.PEER to pJson["peerDependencies"],
            NpmDependency.Scope.OPTIONAL to pJson["optionalDependencies"],
            NpmDependency.Scope.DEV to pJson["devDependencies"],
        ).mapValues { (_, depsEl) ->
            depsEl?.jsonObject?.map { (k, v) -> k to v.jsonPrimitive.content }?.toMap()
        }.mapNotNull { (scope, deps) ->
            deps?.map { (k, v) -> NpmDependencyDeclaration(scope, k, v) }
        }.flatten().toSet()
    }
}
