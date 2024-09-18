/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// To be compatible with previous KGP version, we need to keep D8RootExtension as deprecated.
// To prevent Kotlin build from failing (due to `-Werror`), only deprecate after upgrade of bootstrap version
//@Deprecated("This extension is deprecated. Use D8Extension instead.", ReplaceWith("D8Extension"))
@OptIn(ExperimentalWasmDsl::class)
typealias D8RootExtension = D8Extension