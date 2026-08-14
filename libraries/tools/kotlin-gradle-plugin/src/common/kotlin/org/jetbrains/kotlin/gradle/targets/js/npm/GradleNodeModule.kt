/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
        val pJson = parsePackageJsonObject(path.resolve("package.json"))
        mapOf(
            NpmDependency.Scope.NORMAL to pJson["dependencies"],
            NpmDependency.Scope.PEER to pJson["peerDependencies"],
            NpmDependency.Scope.OPTIONAL to pJson["optionalDependencies"],
            NpmDependency.Scope.DEV to pJson["devDependencies"],
        ).mapValues { (_, depsEl) ->
            // skip rather than record a "null" version string: JsonNull is itself a JsonPrimitive
            (depsEl as? JsonObject)?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }?.toMap()
        }.mapNotNull { (scope, deps) ->
            deps?.map { (k, v) -> NpmDependencyDeclaration(scope, k, v) }
        }.flatten().toSet()
    }
}
