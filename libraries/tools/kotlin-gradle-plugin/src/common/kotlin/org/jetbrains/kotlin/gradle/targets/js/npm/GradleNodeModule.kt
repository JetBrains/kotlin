/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.JsonObject
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
        val pJson = path.resolve("package.json").reader().use {
            Gson().fromJson(it, JsonObject::class.java)
        }
        val normal = pJson.getAsJsonObject("dependencies")
        val peer = pJson.getAsJsonObject("peerDependencies")
        val optional = pJson.getAsJsonObject("optionalDependencies")
        val dev = pJson.getAsJsonObject("devDependencies")
        mapOf(
            NpmDependency.Scope.NORMAL to normal,
            NpmDependency.Scope.PEER to peer,
            NpmDependency.Scope.OPTIONAL to optional,
            NpmDependency.Scope.DEV to dev
        ).mapValues { (_, deps) ->
            deps?.entrySet()?.associate { (k, v) -> k to v.asString }
        }.mapNotNull { (scope, deps) ->
            deps?.map { (k, v) -> NpmDependencyDeclaration(scope, k, v) }
        }.flatten().toSet()
    }
}
