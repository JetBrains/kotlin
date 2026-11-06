/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels

/**
 * Parameter of a Kotlin import model request, which the IDE passes through the Gradle Tooling API.
 * [kotlinModelId] is one of [KotlinImportModelIds], and [kotlinModelParameters] holds the model's serialized parameters.
 */
interface ModelRequest {
    // Nullable because the Tooling API passes the request as a proxy, which returns null for a property the IDE didn't set
    var kotlinModelId: String?
    var kotlinModelParameters: ByteArray?
}
