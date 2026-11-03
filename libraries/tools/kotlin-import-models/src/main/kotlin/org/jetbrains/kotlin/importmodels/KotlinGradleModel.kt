/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels

import java.io.Serializable

/**
 * Model that the Kotlin Gradle Plugin returns to the IDE for a [ModelRequest].
 * [kotlinModelResult] holds a serialized [org.jetbrains.kotlin.importmodels.proto.Result].
 */
interface KotlinGradleModel : Serializable {
    val kotlinModelResult: ByteArray
}
