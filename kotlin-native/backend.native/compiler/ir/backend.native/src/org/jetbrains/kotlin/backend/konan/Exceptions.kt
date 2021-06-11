/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

/**
 * Represents a compilation error caused by mistakes in an input file, e.g. undefined reference.
 */
class KonanCompilationException(
        message: String = "",
        cause: Throwable? = null
) : KotlinExceptionWithAttachments(message, cause)
