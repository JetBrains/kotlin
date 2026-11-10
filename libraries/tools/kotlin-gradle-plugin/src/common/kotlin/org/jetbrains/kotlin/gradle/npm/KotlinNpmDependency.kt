/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.Serializable

/**
 * An npm dependency declared in a Kotlin source set, e.g. with `npm("is-even", "1.0.0")`.
 */
internal sealed class KotlinNpmDependency {
    /**
     * The npm package name, used as the key in the `package.json` dependencies section.
     *
     * Either an unscoped name, e.g. `is-even`, or a [scoped](https://docs.npmjs.com/cli/v11/using-npm/scope) name,
     * e.g. `@types/node`.
     */
    @get:Input
    abstract val name: String

    /**
     * Determines the `package.json` dependencies section.
     */
    @get:Input
    abstract val dependencyType: DependencyType

    /**
     * The `package.json` section an npm dependency belongs to.
     */
    internal enum class DependencyType {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER,
    }

    /**
     * A dependency on an npm package, identified by a [versionSpec] that the package manager resolves.
     */
    internal data class Package(
        @get:Input
        override val name: String,
        @get:Input
        override val dependencyType: DependencyType,
        /**
         * The [dependency spec](https://docs.npmjs.com/cli/v11/configuring-npm/package-json#dependencies),
         * written verbatim as the value in the `package.json` dependencies section.
         *
         * Might not just be a `1.2.3` version. Npm also supports version ranges, git or GitHub URLs, and tarball URLs...
         */
        @get:Input
        val versionSpec: Provider<String>,
    ) : KotlinNpmDependency(), Serializable

    /**
     * A dependency on an npm package in a local [directory].
     *
     * Written to `package.json` as a [local path](https://docs.npmjs.com/cli/v11/configuring-npm/package-json#local-paths).
     */
    internal data class LocalPath(
        @get:Input
        override val name: String,
        @get:Input
        override val dependencyType: DependencyType,
        @get:InputDirectory
        @get:PathSensitive(RELATIVE)
        val directory: Provider<Directory>,
    ) : KotlinNpmDependency(), Serializable
}
