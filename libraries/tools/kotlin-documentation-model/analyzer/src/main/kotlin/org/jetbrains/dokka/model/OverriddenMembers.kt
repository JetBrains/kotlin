/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

/** Identifiers of directly overridden declarations, by source set. */
internal data class OverriddenMembers(
    val members: SourceSetDependent<List<DRI>>,
) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, OverriddenMembers> {
        override fun mergeStrategyFor(left: OverriddenMembers, right: OverriddenMembers): MergeStrategy<Documentable> =
            MergeStrategy.Replace(OverriddenMembers(left.members + right.members))
    }

    override val key: ExtraProperty.Key<Documentable, *> = OverriddenMembers
}
