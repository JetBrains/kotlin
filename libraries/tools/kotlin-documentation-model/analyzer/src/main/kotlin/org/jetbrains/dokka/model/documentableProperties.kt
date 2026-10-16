/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

internal data class InheritedMember(val inheritedFrom: SourceSetDependent<DRI?>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, InheritedMember> {
        override fun mergeStrategyFor(left: InheritedMember, right: InheritedMember): MergeStrategy<Documentable> = MergeStrategy.Replace(
            InheritedMember(left.inheritedFrom + right.inheritedFrom)
        )
    }

    fun isInherited(sourceSetDependent: DokkaSourceSet): Boolean = inheritedFrom[sourceSetDependent] != null

    override val key: ExtraProperty.Key<Documentable, *> = InheritedMember
}

internal data class ImplementedInterfaces(val interfaces: SourceSetDependent<List<TypeConstructor>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ImplementedInterfaces> {
        override fun mergeStrategyFor(left: ImplementedInterfaces, right: ImplementedInterfaces): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ImplementedInterfaces(left.interfaces + right.interfaces))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ImplementedInterfaces
}

internal data class ExceptionInSupertypes(val exceptions: SourceSetDependent<List<TypeConstructor>>): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ExceptionInSupertypes> {
        override fun mergeStrategyFor(left: ExceptionInSupertypes, right: ExceptionInSupertypes): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ExceptionInSupertypes(left.exceptions + right.exceptions))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ExceptionInSupertypes
}

internal object ObviousMember : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, ObviousMember> {
    override val key: ExtraProperty.Key<Documentable, *> = this
}

/**
 * Marks a callable as belonging to the "companion block" scope of its enclosing classlike,
 * meaning it has no instance receiver from the enclosing class.
 *
 *
 * This covers, in a unified way:
 * - Java `static` methods and fields
 * - Enum synthetic declarations (`values`, `valueOf`, `entries`)
 * - Kotlin companion-block members
 *   (see [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md))
 * - Kotlin companion extensions
 *   (see [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md))
 *
 * The same condition is reflected in the [org.jetbrains.dokka.links.Callable.isCompanion]
 * field of the [DRI] so that companion and instance callables with the same name
 * can be distinguished by their DRI signature.
 */
@ExperimentalDokkaApi
internal object IsCompanion : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, IsCompanion> {
    override val key: ExtraProperty.Key<Documentable, *> = this
}

/**
 * Whether this [DProperty] is `var` or `val`, should be present both in Kotlin and in Java properties
 *
 * In case of properties that came from `Java`, [IsVar] is added if
 * the java field has no accessors at all (plain field) or has a setter
 */
internal object IsVar : ExtraProperty<DProperty>, ExtraProperty.Key<DProperty, IsVar> {
    override val key: ExtraProperty.Key<DProperty, *> = this
}

internal data class IsAlsoParameter(val inSourceSets: List<DokkaSourceSet>) : ExtraProperty<DProperty> {
    companion object : ExtraProperty.Key<DProperty, IsAlsoParameter> {
        override fun mergeStrategyFor(left: IsAlsoParameter, right: IsAlsoParameter): MergeStrategy<DProperty> =
            MergeStrategy.Replace(IsAlsoParameter(left.inSourceSets + right.inSourceSets))
    }

    override val key: ExtraProperty.Key<DProperty, *> = IsAlsoParameter
}

internal data class CheckedExceptions(val exceptions: SourceSetDependent<List<DRI>>) : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, ObviousMember> {
    companion object : ExtraProperty.Key<Documentable, CheckedExceptions> {
        override fun mergeStrategyFor(left: CheckedExceptions, right: CheckedExceptions): MergeStrategy<Documentable> =
            MergeStrategy.Replace(CheckedExceptions(left.exceptions + right.exceptions))
    }
    override val key: ExtraProperty.Key<Documentable, *> = CheckedExceptions
}
