/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet

public fun <T> SourceSetDependent<T>.filtered(sourceSets: Set<DokkaSourceSet>): SourceSetDependent<T> = filter { it.key in sourceSets }
public fun DokkaSourceSet?.filtered(sourceSets: Set<DokkaSourceSet>): DokkaSourceSet? = takeIf { this in sourceSets }

public fun DTypeParameter.filter(filteredSet: Set<DokkaSourceSet>): DTypeParameter? =
    if (filteredSet.containsAll(sourceSets)) this
    else {
        val intersection = filteredSet.intersect(sourceSets)
        if (intersection.isEmpty()) null
        else DTypeParameter(
            variantTypeParameter,
            documentation.filtered(intersection),
            expectPresentInSet?.takeIf { intersection.contains(expectPresentInSet) },
            bounds,
            intersection,
            extra
        )
    }

public fun Documentable.isExtension(): Boolean = this is Callable && receiver != null
