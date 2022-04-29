package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.kotlin.util.firstNotNullResult

internal class DefaultDocumentableMerger(val context: DokkaContext) : DocumentableMerger {
    private val dependencyInfo = context.getDependencyInfo()

    override fun invoke(modules: Collection<DModule>): DModule? =
        modules.reduceOrNull { left, right ->
            val list = listOf(left, right)
            DModule(
                name = modules.map { it.name }.distinct().joinToString("|"),
                packages = merge(
                    list.flatMap { it.packages }
                ) { pck1, pck2 -> pck1.mergeWith(pck2) },
                documentation = list.map { it.documentation }.flatMap { it.entries }.associate { (k, v) -> k to v },
                expectPresentInSet = list.firstNotNullResult { it.expectPresentInSet },
                sourceSets = list.flatMap { it.sourceSets }.toSet()
            ).mergeExtras(left, right)
        }

    private fun DokkaContext.getDependencyInfo()
            : Map<DokkaConfiguration.DokkaSourceSet, List<DokkaConfiguration.DokkaSourceSet>> {

        fun getDependencies(sourceSet: DokkaConfiguration.DokkaSourceSet): List<DokkaConfiguration.DokkaSourceSet> =
            listOf(sourceSet) + configuration.sourceSets.filter {
                it.sourceSetID in sourceSet.dependentSourceSets
            }.flatMap { getDependencies(it) }

        return configuration.sourceSets.associateWith { getDependencies(it) }
    }

    private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
        elements.groupingBy { it.dri }
            .reduce { _, left, right -> reducer(left, right) }
            .values.toList()

    private fun <T> mergeExpectActual(
        elements: List<T>,
        reducer: (T, T) -> T
    ): List<T> where T : Documentable, T : WithSources {

        fun mergeClashingElements(elements: List<Pair<T, Set<DokkaConfiguration.DokkaSourceSet>>>): List<T> =
            elements.groupBy { it.first.name }.values.flatMap { listOfDocumentableToSSIds ->
                val merged = listOfDocumentableToSSIds.map { (documentable, sourceSets) ->
                    when (documentable) {
                        is DClass -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DObject -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DAnnotation -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DInterface -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DEnum -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DFunction -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DProperty -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        else -> documentable
                    }
                }
                @Suppress("UNCHECKED_CAST")
                merged as List<T>
            }


        fun analyzeExpectActual(sameDriElements: List<T>): List<T> {
            val (expects, actuals) = sameDriElements.partition { it.expectPresentInSet != null }
            val groupedByOwnExpectWithActualSourceSetIds = expects.map { expect ->
                val actualsForGivenExpect = actuals.filter { actual ->
                    dependencyInfo[actual.sourceSets.single()]
                        ?.contains(expect.expectPresentInSet!!)
                        ?: throw IllegalStateException("Cannot resolve expect/actual relation for ${actual.name}")
                }
                (listOf(expect) + actualsForGivenExpect) to actualsForGivenExpect.flatMap { it.sourceSets }.toSet()
            }
            val reducedToOneDocumentableWithActualSourceSetIds =
                groupedByOwnExpectWithActualSourceSetIds.map { it.first.reduce(reducer) to it.second }
            return reducedToOneDocumentableWithActualSourceSetIds.let(::mergeClashingElements)
        }


        return elements.partition {
            (it as? WithIsExpectActual)?.isExpectActual ?: false
        }.let { (expectActuals, notExpectActuals) ->
            notExpectActuals.map { it to it.sourceSets }
                .groupBy { it.first.dri }.values.flatMap(::mergeClashingElements) +
                    expectActuals.groupBy { it.dri }.values.flatMap(::analyzeExpectActual)
        }
    }

    fun DPackage.mergeWith(other: DPackage): DPackage = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        typealiases = merge(typealiases + other.typealiases) { ta1, ta2 -> ta1.mergeWith(ta2) },
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DFunction.mergeWith(other: DFunction): DFunction = copy(
        parameters = merge(this.parameters + other.parameters) { p1, p2 -> p1.mergeWith(p2) },
        receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        modifier = modifier + other.modifier,
        sourceSets = sourceSets + other.sourceSets,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
    ).mergeExtras(this, other)

    fun DProperty.mergeWith(other: DProperty): DProperty = copy(
        receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        modifier = modifier + other.modifier,
        sourceSets = sourceSets + other.sourceSets,
        getter = getter?.let { g -> other.getter?.let { g.mergeWith(it) } ?: g } ?: other.getter,
        setter = setter?.let { s -> other.setter?.let { s.mergeWith(it) } ?: s } ?: other.setter,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
    ).mergeExtras(this, other)

    fun DClasslike.mergeWith(other: DClasslike): DClasslike = when {
        this is DClass && other is DClass -> mergeWith(other)
        this is DEnum && other is DEnum -> mergeWith(other)
        this is DInterface && other is DInterface -> mergeWith(other)
        this is DObject && other is DObject -> mergeWith(other)
        this is DAnnotation && other is DAnnotation -> mergeWith(other)
        else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot be merged with ${other::class.qualifiedName} ${other.name}")
    }

    fun DClass.mergeWith(other: DClass): DClass = copy(
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        modifier = modifier + other.modifier,
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DEnum.mergeWith(other: DEnum): DEnum = copy(
        entries = merge(entries + other.entries) { ee1, ee2 -> ee1.mergeWith(ee2) },
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DEnumEntry.mergeWith(other: DEnumEntry): DEnumEntry = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DObject.mergeWith(other: DObject): DObject = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DInterface.mergeWith(other: DInterface): DInterface = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DAnnotation.mergeWith(other: DAnnotation): DAnnotation = copy(
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) }
    ).mergeExtras(this, other)

    fun DParameter.mergeWith(other: DParameter): DParameter = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DTypeParameter.mergeWith(other: DTypeParameter): DTypeParameter = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    fun DTypeAlias.mergeWith(other: DTypeAlias): DTypeAlias = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        underlyingType = underlyingType + other.underlyingType,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)
}

data class ClashingDriIdentifier(val value: Set<DokkaConfiguration.DokkaSourceSet>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ClashingDriIdentifier> {
        override fun mergeStrategyFor(
            left: ClashingDriIdentifier,
            right: ClashingDriIdentifier
        ): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ClashingDriIdentifier(left.value + right.value))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ClashingDriIdentifier
}
