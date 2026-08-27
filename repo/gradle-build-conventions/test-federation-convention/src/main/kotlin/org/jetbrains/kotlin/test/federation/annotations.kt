/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is temporary/transitional and will be removed/replaced in the future"
)
annotation class TemporaryTestFederationApi

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is delicate: Use only deliberately with consent of the Kotlin Infrastructure Team"
)
annotation class DelicateTestFederationApi
