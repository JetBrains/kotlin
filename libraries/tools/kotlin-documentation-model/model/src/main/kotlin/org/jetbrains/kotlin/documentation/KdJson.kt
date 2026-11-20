/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.json.Json

/**
 * The [Json] format used to (de)serialize [KdBundle] and other KDM model types, with the
 * polymorphic `kind` discriminator used by [KdElement] and other sealed model hierarchies.
 */
val KdJson: Json = Json {
    classDiscriminator = "kind"
    ignoreUnknownKeys = true
}
