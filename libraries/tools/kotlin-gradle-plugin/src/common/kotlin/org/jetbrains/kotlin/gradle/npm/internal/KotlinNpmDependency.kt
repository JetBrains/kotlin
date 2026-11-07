/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm.internal

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
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
     * (An npm package scope is unrelated to the dependency [scope].)
     */
    abstract val name: String

    /**
     * Determines the `package.json` dependencies section.
     */
    // TODO KT-91346 rename to `dependencyType` (matches npm's --omit/--include "dependency types"). 'Scope' in npm means the `@x/` prefix https://docs.npmjs.com/cli/v11/using-npm/scope
    abstract val scope: Scope

    /**
     * The `package.json` section an npm dependency belongs to.
     */
    enum class Scope {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER
    }

    /**
     * A dependency on an npm package, identified by a [versionSpec] that the package manager resolves.
     */
    data class Package(
        override val name: String,
        override val scope: Scope,
        /**
         * The [dependency spec](https://docs.npmjs.com/cli/v11/configuring-npm/package-json#dependencies),
         * written verbatim as the value in the `package.json` dependencies section.
         *
         * Might not just be a `1.2.3` version. Npm also supports version ranges, git or GitHub URLs, and tarball URLs...
         */
        val versionSpec: Provider<String>,
    ) : KotlinNpmDependency(), Serializable

    /**
     * A dependency on an npm package in a local [directory].
     *
     * Written to `package.json` as a [local path](https://docs.npmjs.com/cli/v11/configuring-npm/package-json#local-paths).
     */
    data class LocalPath(
        override val name: String,
        override val scope: Scope,
        val directory: Provider<Directory>,
    ) : KotlinNpmDependency(), Serializable
}
