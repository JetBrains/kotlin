/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

public data class InheritedMember(val inheritedFrom: SourceSetDependent<DRI?>) : ExtraProperty<Documentable> {
    public companion object : ExtraProperty.Key<Documentable, InheritedMember> {
        override fun mergeStrategyFor(left: InheritedMember, right: InheritedMember): MergeStrategy<Documentable> = MergeStrategy.Replace(
            InheritedMember(left.inheritedFrom + right.inheritedFrom)
        )
    }

    public fun isInherited(sourceSetDependent: DokkaSourceSet): Boolean = inheritedFrom[sourceSetDependent] != null

    override val key: ExtraProperty.Key<Documentable, *> = InheritedMember
}

public data class ImplementedInterfaces(val interfaces: SourceSetDependent<List<TypeConstructor>>) : ExtraProperty<Documentable> {
    public companion object : ExtraProperty.Key<Documentable, ImplementedInterfaces> {
        override fun mergeStrategyFor(left: ImplementedInterfaces, right: ImplementedInterfaces): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ImplementedInterfaces(left.interfaces + right.interfaces))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ImplementedInterfaces
}

public data class ExceptionInSupertypes(val exceptions: SourceSetDependent<List<TypeConstructor>>): ExtraProperty<Documentable> {
    public companion object : ExtraProperty.Key<Documentable, ExceptionInSupertypes> {
        override fun mergeStrategyFor(left: ExceptionInSupertypes, right: ExceptionInSupertypes): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ExceptionInSupertypes(left.exceptions + right.exceptions))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ExceptionInSupertypes
}

public object ObviousMember : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, ObviousMember> {
    override val key: ExtraProperty.Key<Documentable, *> = this
}

/**
 * Whether this [DProperty] is `var` or `val`, should be present both in Kotlin and in Java properties
 *
 * In case of properties that came from `Java`, [IsVar] is added if
 * the java field has no accessors at all (plain field) or has a setter
 */
public object IsVar : ExtraProperty<DProperty>, ExtraProperty.Key<DProperty, IsVar> {
    override val key: ExtraProperty.Key<DProperty, *> = this
}

public data class IsAlsoParameter(val inSourceSets: List<DokkaSourceSet>) : ExtraProperty<DProperty> {
    public companion object : ExtraProperty.Key<DProperty, IsAlsoParameter> {
        override fun mergeStrategyFor(left: IsAlsoParameter, right: IsAlsoParameter): MergeStrategy<DProperty> =
            MergeStrategy.Replace(IsAlsoParameter(left.inSourceSets + right.inSourceSets))
    }

    override val key: ExtraProperty.Key<DProperty, *> = IsAlsoParameter
}

public data class CheckedExceptions(val exceptions: SourceSetDependent<List<DRI>>) : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, ObviousMember> {
    public companion object : ExtraProperty.Key<Documentable, CheckedExceptions> {
        override fun mergeStrategyFor(left: CheckedExceptions, right: CheckedExceptions): MergeStrategy<Documentable> =
            MergeStrategy.Replace(CheckedExceptions(left.exceptions + right.exceptions))
    }
    override val key: ExtraProperty.Key<Documentable, *> = CheckedExceptions
}
