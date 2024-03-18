/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

/**
 * Marks an API that is still experimental in Binary compatibility validator and may change
 * in the future. There are also no guarantees on preserving the behavior of the API until its
 * stabilization.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalBCVApi
