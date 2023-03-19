/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

/**
 * A generic exception that indicates problems with metadata deserialization.
 */
internal class InconsistentKotlinMetadataException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
