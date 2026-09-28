/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.utils

import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Filters out `kotlin.Any`, an implicit supertype of every Kotlin class that is not worth documenting.
 */
internal fun Sequence<KaType>.filterOutAny(): Sequence<KaType> =
    filterNot { (it as? KaClassType)?.classId == StandardClassIds.Any }
