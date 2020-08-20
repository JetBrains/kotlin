package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class DefaultDocumentableMerger(val context: DokkaContext) : DocumentableMerger {
    private val dependencyInfo = context.getDependencyInfo()


    override fun invoke(modules: Collection<DModule>): DModule {

        return topologicalSort(modules).reduce { left, right ->
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
    }

    private fun topologicalSort(allModules: Collection<DModule>): List<DModule> {

        val modulesMap: Map<DokkaSourceSetID, ModuleOfDifferentTranslators> =
            allModules.groupBy { it.sourceSets.single().sourceSetID }

        //this returns representation of graph where directed edges are leading from module to modules that depend on it
        val graph: Map<ModuleOfDifferentTranslators?, List<ModuleOfDifferentTranslators>> = modulesMap.flatMap { (_, module) ->
            module.first().sourceSets.single().dependentSourceSets.map { sourceSet ->
                modulesMap[sourceSet] to module
            }
        }.groupBy { it.first }.entries
            .map { it.key to it.value.map { it.second } }
            .toMap()


        val visited = modulesMap.map { it.value to false }.toMap().toMutableMap()
        val topologicalSortedList: MutableList<ModuleOfDifferentTranslators> = mutableListOf()

        fun dfs(module: ModuleOfDifferentTranslators) {
            visited[module] = true
            graph[module]?.forEach { if (!visited[it]!!) dfs(it) }
            topologicalSortedList.add(0, module)
        }
        modulesMap.values.forEach { if (!visited[it]!!) dfs(it) }

        return topologicalSortedList.flatten()
    }

    private fun DokkaContext.getDependencyInfo()
            : Map<DokkaConfiguration.DokkaSourceSet, List<DokkaConfiguration.DokkaSourceSet>> {

        fun getDependencies(sourceSet: DokkaConfiguration.DokkaSourceSet): List<DokkaConfiguration.DokkaSourceSet> =
            listOf(sourceSet) + configuration.sourceSets.filter {
                it.sourceSetID in sourceSet.dependentSourceSets
            }.flatMap { getDependencies(it) }

        return configuration.sourceSets.map { it to getDependencies(it) }.toMap()
    }

    private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
        elements.groupingBy { it.dri }
            .reduce { _, left, right -> reducer(left, right) }
            .values.toList()

    private fun <T> mergeExpectActual(
        elements: List<T>,
        reducer: (T, T) -> T
    ): List<T> where T : Documentable, T : WithExpectActual {

        fun T.isExpectActual(): Boolean =
            this.safeAs<WithExtraProperties<T>>().let { it != null && it.extra[IsExpectActual] != null }

        fun Set<DokkaConfiguration.DokkaSourceSet>.parentSourceSet(): String = singleOrNull {
            it.dependentSourceSets.all { it !in this.map { it.sourceSetID } }
        }?.displayName
            ?: "unresolved".also { context.logger.error("Ill-defined dependency between sourceSets") }

        fun mergeClashingElements(elements: List<T>): List<T> = elements.groupBy { it.name }.values.flatMap {
            if(it.size > 1) it.map {
                when(it) {
                    is DClass -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DObject -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DAnnotation -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DInterface -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DEnum -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DFunction -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    is DProperty -> it.copy(name = "${it.name}(${it.sourceSets.parentSourceSet()})")
                    else -> it
                }
            } as List<T> else it
        }

        fun analyzeExpectActual(sameDriElements: List<T>): List<T> {
            val (expects, actuals) = sameDriElements.partition { it.expectPresentInSet != null }
            val groupedByOwnExpect = expects.map { expect ->
                listOf(expect) + actuals.filter { actual ->
                    dependencyInfo[actual.sourceSets.single()]
                        ?.contains(expect.expectPresentInSet!!)
                        ?: throw IllegalStateException("Cannot resolve expect/actual relation for ${actual.name}")
                }
            }
            val reducedToOneDocumentable = groupedByOwnExpect.map { it.reduce(reducer) }
            val uniqueNamedDocumentables = reducedToOneDocumentable.let(::mergeClashingElements)
            return uniqueNamedDocumentables
        }


        return elements.partition {
            it.isExpectActual()
        }.let { (expectActuals, notExpectActuals) ->
            notExpectActuals.groupBy { it.dri }.values.flatMap(::mergeClashingElements) +
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

private typealias ModuleOfDifferentTranslators = List<DModule>
