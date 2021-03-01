package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedMember(val inheritedFrom: SourceSetDependent<DRI?>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, InheritedMember> {
        override fun mergeStrategyFor(left: InheritedMember, right: InheritedMember) = MergeStrategy.Replace(
            InheritedMember(left.inheritedFrom + right.inheritedFrom)
        )
    }

    fun isInherited(sourceSetDependent: DokkaSourceSet): Boolean = inheritedFrom[sourceSetDependent] != null

    override val key: ExtraProperty.Key<Documentable, *> = InheritedMember
}

data class ImplementedInterfaces(val interfaces: SourceSetDependent<List<TypeConstructor>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ImplementedInterfaces> {
        override fun mergeStrategyFor(left: ImplementedInterfaces, right: ImplementedInterfaces) =
            MergeStrategy.Replace(ImplementedInterfaces(left.interfaces + right.interfaces))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ImplementedInterfaces
}

data class ExceptionInSupertypes(val exceptions: SourceSetDependent<List<TypeConstructor>>): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ExceptionInSupertypes> {
        override fun mergeStrategyFor(left: ExceptionInSupertypes, right: ExceptionInSupertypes) =
            MergeStrategy.Replace(ExceptionInSupertypes(left.exceptions + right.exceptions))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ExceptionInSupertypes
}

object ObviousMember : ExtraProperty<Documentable>, ExtraProperty.Key<Documentable, ObviousMember> {
    override val key: ExtraProperty.Key<Documentable, *> = this
}