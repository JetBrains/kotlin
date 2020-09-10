/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val packages = VersionFetcher(coroutineContext).fetch()
        findLastVersions(packages)
            .forEach {
                println(it)
            }
    }
}

fun findLastVersions(packages: List<PackageInformation>): List<Package> {
    return packages
        .map { packageInformation ->
            val maximumVersion = packageInformation.versions
                .map { SemVer.from(it) }
                .filter { it.preRelease == null && it.build == null }
                .maxOrNull() ?: throw error("There is no applicable version for ${packageInformation.name}")

            Package(
                packageInformation.name,
                maximumVersion
            )
        }
}