/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.jetbrains.annotations.ApiStatus

/**
 * Marks the old npm dependency model: Node.js and npm/Yarn tooling set up in the root project,
 * npm dependencies collected and installed through the root project.
 *
 * It is incompatible with Isolated Projects, and is being replaced by the shared npm project
 * ([KotlinSharedNpmProjectPlugin]) and the Node.js toolchain (KT-80311).
 *
 * Don't use it in new code.
 */
internal typealias ObsoleteNpmDependencyModel = ApiStatus.Obsolete
