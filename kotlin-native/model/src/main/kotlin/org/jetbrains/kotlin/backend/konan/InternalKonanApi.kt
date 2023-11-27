/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

@RequiresOptIn(
        "This API is internal to the Kotlin/Native compiler and cannot be used outside of kotlin.git",
        level = RequiresOptIn.Level.ERROR
)
annotation class InternalKonanApi()
