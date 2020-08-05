/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmRangeVisitor.Companion.WILDCARD

class NpmVersionConstraint(
    version: String
) : DefaultMutableVersionConstraint(version) {
    override fun rejectAll() {
        super.rejectAll()
        reject(WILDCARD)
    }

    fun toSemVer(): String {
        if (requiredVersion.isEmpty()) {
            return buildNpmVersion(
                includedVersions = listOf(preferredVersion),
                excludedVersions = rejectedVersions
            )
        }

        if (strictVersion.isNotEmpty()) {
            return buildNpmVersion(
                includedVersions = listOf(strictVersion),
                excludedVersions = rejectedVersions
            )
        }

        return buildNpmVersion(
            listOf(requiredVersion),
            rejectedVersions,
            true
        )
    }
}