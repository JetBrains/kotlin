/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.jetbrains.annotations.ApiStatus

/**
 * Marks 'RootResolver' npm dependency management: Node.js and npm/Yarn tooling set up in the root project,
 * npm dependencies collected and installed through the root project.
 *
 * 'RootResolver' npm dependency management is incompatible with Isolated Projects,
 * and is being replaced by 'SharedProject' npm dependency management
 * ([KotlinSharedNpmProjectPlugin]) and the Node.js toolchain (KT-80311, KT-91120).
 */
internal typealias ObsoleteNpmDependencyModel = ApiStatus.Obsolete
