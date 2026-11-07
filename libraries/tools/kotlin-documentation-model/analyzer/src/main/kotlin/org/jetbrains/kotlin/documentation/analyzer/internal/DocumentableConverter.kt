/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.analyzer.KdAnalyzerSourceSet

/**
 * Converts a single, merged [DModule] (the result of the internal Dokka analysis/merge pipeline, see
 * [runDokkaAnalysis]) into a [KdBundle]: one [KdFragment] per requested source set.
 */
internal class DocumentableConverter(
    sourceSets: List<KdAnalyzerSourceSet>,
    private val module: DModule,
    private val context: DokkaContext,
    // Opt in to materializing dependency classes referenced by superTypes, including their ancestor hierarchy.
    private val embedSuperTypes: Boolean = true,
) {
    private val graph = SourceSetGraph(sourceSets)
    private val fragmentNames = sourceSets.map { it.name }

    /**
     * A member's [InheritedMember.inheritedFrom] whose (package, class) isn't in this set points at a declaration
     * outside the analyzed module (JDK, Kotlin stdlib, any other classpath dependency), which needs
     * [KdFragment.embeddedElements] support.
     */
    private val internalClasslikeDris: Set<Pair<String?, String?>> = buildSet {
        fun visit(node: Documentable) {
            if (node is DClasslike) add(node.dri.packageName to node.dri.classNames)
            node.children.forEach(::visit)
        }
        visit(module)
    }

    private fun isExternalClass(dri: DRI): Boolean = (dri.packageName to dri.classNames) !in internalClasslikeDris

    private fun pickDeclaredSourceSetOrDependent(d: Documentable, fragmentName: String): DokkaConfiguration.DokkaSourceSet =
        d.sourceSets.firstOrNull { it.sourceSetID.sourceSetName == fragmentName } ?: // exact source set
        /**
         *
         * ```kotlin
         * // commonMain
         * expect fun f(): Int
         * // jvmMain
         * actual fun f(): Int = 1
         * // nativeMain
         * actual fun f(): Int = 2
         *
         * // linuxMain
         * // Inherits the native implementation unless overridden.
         * ```
         */
        run {
            val reachable = graph.reachableFrom(fragmentName)
            val candidates = d.sourceSets.filter { it.sourceSetID.sourceSetName in reachable }
            candidates.singleOrNull { candidate ->
                val ancestors = graph.reachableFrom(candidate.sourceSetID.sourceSetName)
                candidates.all { it.sourceSetID.sourceSetName in ancestors }
            } ?: error("No unique most specific source set found for fragment $fragmentName in $d")
        }

    fun convert(): KdBundle = KdBundle(
        schemaVersion = KotlinVersion.CURRENT.toString(),
        fragments = fragmentNames.map(::buildFragment),
        expectActualMappings = collectExpectActualMappings(),
    )

    private fun MutableMap<KdElementId, KdElement>.putElement(id: KdElementId, element: KdElement): KdElementId {
        // Inherited members can be visited repeatedly, but their shared ID must always resolve to the same element.
        val previous = putIfAbsent(id, element)
        check(previous == null || previous == element) {
            "Conflicting elements for ID '${id.value}': $previous and $element"
        }
        return id
    }

    private fun isVisibleFromDependentSourceSet(documentable: Documentable, fragmentName: String): Boolean =
        documentable.sourceSets.any { it.sourceSetID.sourceSetName in graph.reachableFrom(fragmentName) }

    private fun pickDeclaredSourceSetOrNull(
        nodeSourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
        fragmentName: String,
    ): DokkaConfiguration.DokkaSourceSet? =
        nodeSourceSets.firstOrNull { it.sourceSetID.sourceSetName == fragmentName }

    private fun buildFragment(fragmentName: String): KdFragment {
        val elements = linkedMapOf<KdElementId, KdElement>()
        val embeddedElements = linkedMapOf<KdElementId, KdElement>()

        if (isVisibleFromDependentSourceSet(module, fragmentName)) {
            val packageIds = module.packages.mapNotNull { visitPackage(it, fragmentName, elements, embeddedElements) }
            val element = KdModule(
                name = module.name,
                packages = packageIds,
                annotations = pickDeclaredSourceSetOrNull(module.sourceSets, fragmentName)
                    ?.let { module.extra.annotationsIn(it) }.orEmpty(),
                documentation = KdDocumentation.EMPTY,
            )
            elements.putElement(module.dri.toKdElementId(), element)
        }

        // embed external elements
        if (embedSuperTypes) {
            embedSuperTypes(fragmentName, elements, embeddedElements)
        }

        return KdFragment(fragmentName, elements, embeddedElements)
    }

    private fun embedSuperTypes(
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
    ) {
        val provider = context.plugin<KotlinAnalysisPlugin>().querySingle { externalDocumentableProvider }
        val visited = mutableSetOf<Pair<DRI, DokkaConfiguration.DokkaSourceSet>>()
        fun visit(node: Documentable) {
            if (node is WithSupertypes) {
                val ss = pickDeclaredSourceSetOrNull(node.supertypes.keys, fragmentName)
                if (ss != null) {
                    for (dri in node.supertypes.getValue(ss).map { it.typeConstructor.dri }) {
                        if (!isExternalClass(dri) || !visited.add(dri to ss)) continue
                        val external = provider.getClasslike(dri, ss) ?: continue
                        val id = dri.toKdElementId()
                        if (id !in elements && id !in embeddedElements) {
                            val members = linkedMapOf<KdElementId, KdElement>()
                            val declarations = visitClasslikeChildren(external, fragmentName, members, members)
                            // Existing inherited members may already have type arguments substituted for the subclass.
                            members.forEach { entry -> embeddedElements.putIfAbsent(entry.key, entry.value) }
                            embeddedElements.putElement(id, buildClassElement(external, ss, declarations, isCompanion = false))
                        }
                        visit(external)
                    }
                }
            }
            node.children.forEach(::visit)
        }
        visit(module)
        require(embeddedElements.keys.none { it in elements })
    }

    fun assumeUniqueDRIinEachSourceSet(documentables: List<Documentable>): Boolean {
        /** TODO case
         *
         *```
         * // coomonMain
         * expect class A
         * //intermediateMain
         * ...
         * // jvmMain
         * class B
         * actual typealias A = B
         * ```
         * common A has jvm
         * intermediate A has jvm
         */
        val filteredDocumentables = documentables.filter { (it as? DClasslike)?.let { node -> actualTypealias(node) } == null }

        val seen = mutableSetOf<Pair<DRI, DokkaSourceSetID>>()
        return filteredDocumentables.all { documentable ->
            documentable.sourceSets.all { sourceSet ->
                seen.add(documentable.dri to sourceSet.sourceSetID)
            }
        }
    }

    private fun visitPackage(
        pkg: DPackage,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
    ): KdElementId? {
        if (!isVisibleFromDependentSourceSet(pkg, fragmentName)) return null

        require(assumeUniqueDRIinEachSourceSet(pkg.children))

        val declarations = mutableListOf<KdElementId>()

        pkg.functions.forEach { visitFunction(it, fragmentName, elements, embeddedElements)?.let(declarations::add) }
        pkg.properties.forEach { visitProperty(it, fragmentName, elements, embeddedElements)?.let(declarations::add) }
        pkg.classlikes.forEach { visitClasslike(it, fragmentName, elements, embeddedElements)?.let(declarations::add) }
        pkg.typealiases.forEach { visitTypealias(it, fragmentName, elements)?.let(declarations::add) }

        // The root package and the module have the same textual DRI.
        val id = if (pkg.packageName.isEmpty()) KdElementId("package:${pkg.dri}") else pkg.dri.toKdElementId()
        val element = KdPackage(
            name = pkg.packageName,
            declarations = declarations,
            annotations = pickDeclaredSourceSetOrDependent(pkg, fragmentName)
                .let { pkg.extra.annotationsIn(it) },
            documentation = KdDocumentation.EMPTY,
        )
        elements.putElement(id, element)
        return id
    }

    private fun <T : Documentable> addDeclaration(
        node: T,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        /**
         * Additional constraint for classlikes,
         * since classlikes and their members are always taken from the source set they are declared in
         *
         * If true, the declaration will be taken only from the source set it is declared in
         *
         * If false, the declaration will be taken from the source set it is declared in and from dependent source sets.
         * For example, top-level callables, top-level typealiases, etc.
         */
        shouldBeTakenFromDeclaredSourceSets: Boolean = false,
        build: (T, DokkaConfiguration.DokkaSourceSet) -> KdElement,
    ): KdElementId? {
        if (shouldBeTakenFromDeclaredSourceSets) {
            val declaredSourceSet = pickDeclaredSourceSetOrNull(node.sourceSets, fragmentName) ?: return null
            return elements.putElement(node.dri.toKdElementId(), build(node, declaredSourceSet))
        } else if (!isVisibleFromDependentSourceSet(node, fragmentName)) return null

        return elements.putIfRefined(node, fragmentName)
            ?: elements.putElement(node.dri.toKdElementId(), build(node, pickDeclaredSourceSetOrDependent(node, fragmentName)))
    }

    private fun <T : Documentable> MutableMap<KdElementId, KdElement>.putIfRefined(
        node: T,
        fragmentName: String,
    ): KdElementId? {
        val ownerName = pickDeclaredSourceSetOrNull(node.sourceSets, fragmentName)
        if (ownerName != null) return null
        val original = pickDeclaredSourceSetOrDependent(node, fragmentName).sourceSetID.sourceSetName
        val id = node.dri.toKdElementId()
        putElement(node.dri.toKdElementId(), KdRefinedElement(original))
        return id
    }

    /**
     * [c] is every raw Dokka sibling sharing [DClasslike.dri]: normally just one, but
     * reanalysing common sources can provide a class-like declaration in every source set from which it is accessible
     */
    private fun visitClasslike(
        c: DClasslike,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
        isCompanion: Boolean = false,
    ): KdElementId? {
        val ownSourceSet = pickDeclaredSourceSetOrNull(c.sourceSets, fragmentName) ?: return null

        val actualTypealias = actualTypealias(c)
        if (actualTypealias?.typeAlias?.sourceSets?.any { it.sourceSetID.sourceSetName == fragmentName } == true) {
            return null
        }

        val id = c.dri.toKdElementId()
        val childIds = visitClasslikeChildren(c, fragmentName, elements, embeddedElements)

        val element = buildClassElement(c, ownSourceSet, childIds, isCompanion)

        return elements.putElement(id, element)
    }

    private fun actualTypealias(node: DClasslike): ActualTypealias? = node.extra[ActualTypealias]

    private fun visitClasslikeChildren(
        node: DClasslike,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
    ): List<KdElementId> = buildList {
        val companionDri = (node as? WithCompanion)?.companion?.dri
        val sourceSetName = pickDeclaredSourceSetOrDependent(node, fragmentName).sourceSetID.sourceSetName

        fun Documentable.isPresent(): Boolean = sourceSets.any { it.sourceSetID.sourceSetName == sourceSetName }

        (node as? DEnum)?.entries?.forEach { visitEnumEntry(it, fragmentName, elements)?.let(::add) }
        (node as? WithConstructors)?.constructors?.filter { it.isPresent() }?.forEach {
            visitConstructor(it, fragmentName, elements)?.let(::add)
        }
        node.functions.filter { it.isPresent() }.forEach {
            visitFunction(it, fragmentName, elements, embeddedElements, node.dri)?.let(::add)
        }
        node.properties.filter { it.isPresent() }.forEach {
            visitProperty(it, fragmentName, elements, embeddedElements, node.dri)?.let(::add)
        }
        node.classlikes.forEach {
            visitClasslike(
                it, fragmentName, elements, embeddedElements,
                isCompanion = it.dri == companionDri,
            )?.let(::add)
        }
        (node as? WithTypealiases)?.typealiases?.forEach { visitTypealias(it, fragmentName, elements)?.let(::add) }
    }

    private fun visitFunction(
        f: DFunction,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
        containingClassDri: DRI? = null,
    ): KdElementId? {
        if (containingClassDri == null) {
            return addDeclaration(f, fragmentName, elements, build = ::buildFunctionElement)
        }
        // members have always source sets from which they are accessible
        val ownerSs = pickDeclaredSourceSetOrNull(f.sourceSets, fragmentName) ?: return null
        val inheritedFrom = f.extra[InheritedMember]?.inheritedFrom?.get(ownerSs)?.takeUnless {
            it.packageName == containingClassDri.packageName && it.classNames == containingClassDri.classNames
        }
        val originalId = f.dri.toKdElementId()

        if (inheritedFrom != null && isExternalClass(inheritedFrom)) {
            val originalId = f.dri.toKdElementId()
            embeddedElements[originalId] = buildFunctionElement(f, ownerSs)
            val perClassId =
                f.dri.copy(packageName = containingClassDri.packageName, classNames = containingClassDri.classNames).toKdElementId()
            elements.putElement(perClassId, KdInheritedMember(originalId))
            return perClassId
        } else if (inheritedFrom != null) {
            val perClassId =
                f.dri.copy(packageName = containingClassDri.packageName, classNames = containingClassDri.classNames).toKdElementId()
            elements.putElement(perClassId, KdInheritedMember(originalId))
            return perClassId
        }

        return elements.putIfRefined(f, fragmentName)
            ?: elements.putElement(f.dri.toKdElementId(), buildFunctionElement(f, ownerSs))
    }

    private fun visitProperty(
        f: DProperty,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
        containingClassDri: DRI? = null,
    ): KdElementId? {
        if (containingClassDri == null) {
            return addDeclaration(f, fragmentName, elements) { f, ss ->
                val accessors = visitAccessors(f, ss, fragmentName, elements, embeddedElements, containingClassDri)
                buildPropertyElement(f, ss, accessors.getter, accessors.setter)
            }
        }
        // members have always source sets from which they are accessible
        val ownerSs = pickDeclaredSourceSetOrNull(f.sourceSets, fragmentName) ?: return null
        val inheritedFrom = f.extra[InheritedMember]?.inheritedFrom?.get(ownerSs)
        val originalId = f.dri.toKdElementId()

        if (inheritedFrom != null && isExternalClass(inheritedFrom)) {
            val accessors = visitAccessors(f, ownerSs, fragmentName, elements, embeddedElements, containingClassDri)
            embeddedElements[originalId] = buildPropertyElement(f, ownerSs, accessors.getter, accessors.setter)
            val perClassId =
                f.dri.copy(packageName = containingClassDri.packageName, classNames = containingClassDri.classNames).toKdElementId()
            elements.putElement(perClassId, KdInheritedMember(originalId))
            return perClassId
        } else if (inheritedFrom != null) {
            val perClassId =
                f.dri.copy(packageName = containingClassDri.packageName, classNames = containingClassDri.classNames).toKdElementId()
            elements.putElement(perClassId, KdInheritedMember(originalId))
            return perClassId
        }

        elements.putIfRefined(f, fragmentName)?.let { return originalId }

        val accessors = visitAccessors(f, ownerSs, fragmentName, elements, embeddedElements, containingClassDri)
        val element = buildPropertyElement(f, ownerSs, accessors.getter, accessors.setter)

        return elements.putElement(originalId, element)
    }

    private fun visitConstructor(c: DFunction, fragmentName: String, elements: MutableMap<KdElementId, KdElement>): KdElementId? =
        addDeclaration(c, fragmentName, elements, shouldBeTakenFromDeclaredSourceSets = true, build = ::buildConstructorElement)

    private fun visitTypealias(
        t: DTypeAlias,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        isMember: Boolean = false,
    ) =
        addDeclaration(t, fragmentName, elements, shouldBeTakenFromDeclaredSourceSets = isMember, build = ::buildTypealiasElement)

    private fun visitEnumEntry(e: DEnumEntry, fragmentName: String, elements: MutableMap<KdElementId, KdElement>) =
        addDeclaration(e, fragmentName, elements, shouldBeTakenFromDeclaredSourceSets = true, build = ::buildEnumEntryElement)

    private data class AccessorIds(val getter: KdElementId?, val setter: KdElementId?)

    private fun visitAccessors(
        p: DProperty,
        ss: DokkaConfiguration.DokkaSourceSet,
        fragmentName: String,
        elements: MutableMap<KdElementId, KdElement>,
        embeddedElements: MutableMap<KdElementId, KdElement>,
        containingClassDri: DRI?,
    ): AccessorIds {
        val getterId = p.getter?.takeIf { isNonDefaultAccessor(it, p, ss) }
            ?.let { visitFunction(it, fragmentName, elements, embeddedElements, containingClassDri) }
        val setterId = p.setter?.takeIf { isNonDefaultAccessor(it, p, ss) }
            ?.let { visitFunction(it, fragmentName, elements, embeddedElements, containingClassDri) }
        return AccessorIds(getterId, setterId)
    }

    /**
     * Whether the accessor needs its own reference to preserve API information beyond the property:
     * inline status, annotations, documentation, or different visibility.
     * A custom body alone does not require a reference; this differs from Analysis API `isNotDefault`.
     */
    private fun isNonDefaultAccessor(accessor: DFunction, property: DProperty, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        if (ExtraModifiers.KotlinOnlyModifiers.Inline in accessor.extra.extraModifiersIn(sourceSet)) return true
        if (accessor.extra.annotationsIn(sourceSet).isNotEmpty()) return true
        if (accessor.documentation[sourceSet] != null) return true
        val accessorVisibility = accessor.visibility[sourceSet]
        val propertyVisibility = property.visibility[sourceSet]
        return accessorVisibility != null && propertyVisibility != null && accessorVisibility != propertyVisibility
    }

    /** Records expect/actual relationships even when Dokka merges their declarations into a single ID. */
    private fun collectExpectActualMappings(): List<KdExpectActualMapping> {
        val expectIds = linkedMapOf<DRI, MutableMap<KdFragmentName, KdElementId>>()
        val actualIds = linkedMapOf<DRI, MutableMap<KdFragmentName, KdElementId>>()
        fun visit(node: Documentable) {
            val actualTypealias = (node as? DClasslike)?.let(::actualTypealias)
            val expectSourceSet = node.expectPresentInSet
            if (expectSourceSet != null && (node as? WithIsExpectActual)?.isExpectActual == true) {
                val id = node.dri.toKdElementId()
                expectIds.getOrPut(node.dri) { linkedMapOf() }[expectSourceSet.sourceSetID.sourceSetName] = id
                val actuals = actualIds.getOrPut(node.dri) { linkedMapOf() }
                for (sourceSet in node.sourceSets - expectSourceSet) {
                    actuals[sourceSet.sourceSetID.sourceSetName] = id
                }
                actualTypealias?.typeAlias?.let { typeAlias ->
                    for (sourceSet in typeAlias.sourceSets) {
                        actuals[sourceSet.sourceSetID.sourceSetName] = typeAlias.dri.toKdElementId()
                    }
                }
            }
            node.children.forEach(::visit)
        }
        visit(module)
        return expectIds.entries.mapNotNull { entry ->
            val expects = entry.value
            val actuals = actualIds.getValue(entry.key) - expects.keys
            if (actuals.isEmpty()) null else KdExpectActualMapping(expects.toMap(), actuals)
        }
    }
}
