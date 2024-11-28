/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

/**
 * Exception thrown when unable to retrieve FUS (Feature Usage Statistics) metrics.
 */

class FusMetricRetrievalException(
    message: String,
    cause: Throwable? = null
): Exception(message, cause)