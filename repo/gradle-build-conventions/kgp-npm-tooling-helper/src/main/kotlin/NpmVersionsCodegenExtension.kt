/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.kgpnpmtooling

import org.gradle.api.file.DirectoryProperty

abstract class NpmVersionsCodegenExtension
internal constructor() {

    /**
     * npm project directory containg a `package.json` file with KGP's npm tooling dependencies
     * and a lockfile.
     */
    abstract val npmToolingProjectDir: DirectoryProperty
}
