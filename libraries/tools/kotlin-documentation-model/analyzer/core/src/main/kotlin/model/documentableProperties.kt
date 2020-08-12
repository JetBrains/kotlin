package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedFunction(val inheritedFrom: SourceSetDependent<DRI?>) : ExtraProperty<DFunction> {
    companion object : ExtraProperty.Key<DFunction, InheritedFunction> {
        override fun mergeStrategyFor(left: InheritedFunction, right: InheritedFunction) = MergeStrategy.Replace(
            InheritedFunction(left.inheritedFrom + right.inheritedFrom)
        )
    }

    fun isInherited(sourceSetDependent: DokkaSourceSet): Boolean = inheritedFrom[sourceSetDependent] != null

    override val key: ExtraProperty.Key<DFunction, *> = InheritedFunction
}

data class ImplementedInterfaces(val interfaces: SourceSetDependent<List<TypeConstructor>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ImplementedInterfaces> {
        override fun mergeStrategyFor(left: ImplementedInterfaces, right: ImplementedInterfaces) =
            MergeStrategy.Replace(ImplementedInterfaces(left.interfaces + right.interfaces))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ImplementedInterfaces
}