/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractorFromRegularKlib
import org.jetbrains.kotlin.backend.konan.serialization.IdSignaturesExtractorFromCInteropKlib
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KLIB_PROPERTY_PACKAGE
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.ir
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.getValue
import kotlin.io.path.pathString

interface KlibDAGNode {
    val library: KotlinLibrary
    val directDependencies: Set<KotlinLibrary>
    val allDependencies: Set<KotlinLibrary>
}

class KlibDAG(private val dag: Map<KotlinLibrary, KlibDAGNode>) {
    val libraries: Set<KotlinLibrary>
        get() = dag.keys

    operator fun get(library: KotlinLibrary): KlibDAGNode =
        dag[library] ?: error("No such library in Klib DAG: $library")
}

class KlibDAGCyclicDependencyException : Exception("Recursive dependency detected while computing DAG of KLIB dependencies")

class KlibDAGBuilder(libraries: Collection<KotlinLibrary>, isRoot: (KotlinLibrary) -> Boolean) {
    private val worker = KlibDAGBuilderImpl(libraries, isRoot)

    /** Cache the result of the DAG computation to now compute it on each [build] invocation. */
    private val result by lazy { worker.build() }

    fun build(): KlibDAG = result
}

private class KlibDAGBuilderImpl(libraries: Collection<KotlinLibrary>, isRoot: (KotlinLibrary) -> Boolean) {
    private var stdlib: KotlinLibrary? = null
    private val rootsButStdlib: MutableList<KotlinLibrary> = mutableListOf()
    private val others: MutableList<KotlinLibrary> = mutableListOf()

    init {
        val librariesByUniquePath = hashMapOf<String, KotlinLibrary>()

        for (library in libraries) {
            // Prevention of occasional duplicates.
            val uniquePath = library.canonicalPath.pathString
            when (val duplicate = librariesByUniquePath[uniquePath]) {
                null -> librariesByUniquePath[uniquePath] = library
                else -> error(
                    """
                        Duplicated libraries found:
                        - $library
                        - $duplicate
                    """.trimIndent()
                )
            }

            // Put the library to the appropriate group.
            when {
                library.isNativeStdlib -> stdlib = library
                isRoot(library) -> rootsButStdlib += library
                else -> others += library
            }
        }
    }

    private val signatureExtractors: Map<KotlinLibrary, IdSignaturesExtractor> = buildMap {
        for (library in libraries) {
            this[library] = when {
                library.isNativeStdlib -> continue
                library.isCInteropLibrary() -> IdSignaturesExtractorFromCInteropKlib(library)
                library.ir != null -> IdSignaturesExtractorFromRegularKlib(library)
                else -> error("This library does not have IR and is not a C-interop library: ${library.path}")
            }
        }
    }

    private val dagUnderConstruction: Map<KotlinLibrary, KlibDAGNodeImpl> = libraries.associateWith(::KlibDAGNodeImpl)

    // Optimization: Stdlib is a dependency for each library. We don't need to extract signatures from it.
    private val stdlibNode: KlibDAGNodeImpl? = stdlib?.let(dagUnderConstruction::get)

    /**
     * Index: contributed package FQNs -> KLIB.
     *
     * This index is intended to speed up the process of DAG dependency building: Platform C-interop libraries
     * may potentially be excluded from the resulting DAG. We need to check if there are declarations from this
     * library that are used somewhere. First of all, before reading IR or metadata (which is expensive), we can
     * check which packages are contributed by this library.
     */
    private val contributedPackageToNode: MutableMap<FqName, MutableSet<KlibDAGNodeImpl>> = hashMapOf()

    // Index: declared signature -> KLIB.
    private val declaredSignatureToNode: MutableMap<IdSignature, KlibDAGNodeImpl> = hashMapOf()

    // Index: KLIB -> imported signatures.
    private val nodeToImportedSignatures: MutableMap<KlibDAGNodeImpl, Set<IdSignature>> = hashMapOf()

    fun build(): KlibDAG {
        // Optimization: Stdlib is a dependency for each library. We don't need to extract signatures from it.
        stampStdlibNodeAsDependencyForEveryone()

        for (library in rootsButStdlib) {
            populateSignatureIndicesForLibrary(library) // Populate indices.
        }

        for (library in others) {
            // Don't populate indices with the expensive data that not necessarily will be used.
            // Instead, memoize the packages represented by a library.
            if (!populateContributedPackageIndexForLibrary(library)) {
                // Fallback to expensive population of signature indices.
                populateSignatureIndicesForLibrary(library)
            }
        }

        // Maintain the set of really used DAG nodes and their statuses.
        val usedNodes: LinkedHashMap<KlibDAGNodeImpl, State> = linkedMapOf()

        // Memoize stdlib as already processed node.
        stdlibNode?.let { usedNodes[it] = State.ALREADY_PROCESSED }

        // Schedule all roots to be processed.
        for (library in rootsButStdlib) {
            usedNodes[dagUnderConstruction.getValue(library)] = State.SCHEDULED_FOR_PROCESSING
        }

        outer@ while (true) {
            // Fetch the last "used" node that has not been processed yet.
            // The last node in LinkedHashMap should be the latest added node, so this would help us to prevent O(n^2).
            val node: KlibDAGNodeImpl = usedNodes.entries.lastOrNull { it.value == State.SCHEDULED_FOR_PROCESSING }?.key ?: break@outer

            fun recordDependency(dependencyNode: KlibDAGNodeImpl) {
                // Make sure that the found node is in the set of used nodes.
                usedNodes.getOrPut(dependencyNode) {
                    // If the found node is new, schedule it for processing on next iterations of the loop.
                    State.SCHEDULED_FOR_PROCESSING
                }

                // Record it as a dependency.
                node.targets += dependencyNode
            }

            for (importedSignature in nodeToImportedSignatures.getValue(node)) {
                when (val dependencyNode: KlibDAGNodeImpl? = declaredSignatureToNode[importedSignature]) {
                    null -> {
                        /**
                         * No dependency found. This may happen due to several reasons:
                         * 1. [importedSignature] is a signature from stdlib, which is intentionally not indexed.
                         * 2. [importedSignature] is a signature of some declaration that is not available.
                         *    This is a legal situation that should be covered by the Partial Linkage engine.
                         * 3. [importedSignature] is a signature from one of non-root libraries, for which [nodeToImportedSignatures]
                         *    and [declaredSignatureToNode] indices have not been populated yet. But there should be a record in
                         *    [contributedPackageToNode] index.
                         *
                         * In practice, it makes sense to check here only case #3.
                         */
                        val maybeDependencyNodes = contributedPackageToNode[importedSignature.packageFqName()]
                        if (!maybeDependencyNodes.isNullOrEmpty()) {
                            inner@ for (maybeDependencyNode in maybeDependencyNodes) {
                                if (populateSignatureIndicesForLibrary(maybeDependencyNode.library)) {
                                    // If we are here, then the indices just have been populated, and it makes sense to check them.
                                    declaredSignatureToNode[importedSignature]?.let { dependencyNodeV2 ->
                                        recordDependency(dependencyNodeV2)
                                        break@inner
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        /** A dependency is found. */
                        recordDependency(dependencyNode)
                    }
                }
            }

            // Mark the node as processed.
            usedNodes[node] = State.ALREADY_PROCESSED
        }

        // Select only the subset of actually used nodes.
        return KlibDAG(dagUnderConstruction.filterValues { it in usedNodes })
    }

    private fun stampStdlibNodeAsDependencyForEveryone() {
        if (stdlibNode != null) {
            for (node in dagUnderConstruction.values) {
                if (node != stdlibNode) node.targets += stdlibNode
            }
        }
    }

    /**
     * Populates [contributedPackageToNode] index.
     *
     * This index is intended to speed up the process of DAG dependency building: Platform C-interop libraries
     * may potentially be excluded from the resulting DAG. We need to check if there are declarations from this
     * library that are used somewhere. First of all, before reading IR or metadata (which is expensive), we can
     * check which packages are contributed by this library.
     *
     * @return `true` only if the index has been actually populated. `false` otherwise.
     */
    private fun populateContributedPackageIndexForLibrary(library: KotlinLibrary): Boolean {
        if (!library.isCInteropLibrary()) return false // Not populated.

        val node = dagUnderConstruction.getValue(library)

        // Interop Klibs may declare only one package, and its FQ name is declared in the manifest.
        val contributedPackageName = library.packageFqName?.let(::FqName)
            ?: error("Interop klib ${library.path} does not contain an expected manifest property: $KLIB_PROPERTY_PACKAGE")

        contributedPackageToNode.getOrPut(contributedPackageName) { hashSetOf() } += node

        return true // Populated.
    }

    /**
     * Populates [declaredSignatureToNode] and [nodeToImportedSignatures] indices.
     *
     * @return `true` if the indices have been populated as a result of this [populateSignatureIndicesForLibrary] call.
     *         `false` if the indices have been already populated earlier.
     */
    private fun populateSignatureIndicesForLibrary(library: KotlinLibrary): Boolean {
        val node = dagUnderConstruction.getValue(library)
        if (node in nodeToImportedSignatures) return false // The indices were populated earlier.

        // Note: We are intentionally extracting only signatures of top-level declarations. It's an optimization.
        // We can always deduce the signature of a top-level class from a signature of any member or an inner/nested class.
        // In case there are numerous members or inner/nested classes, this helps us to reduce the amount of the computational work.
        val [declaredSignatures, importedSignatures] = signatureExtractors.getValue(library).extractOnlyTopLevelPublicSignatures()

        for (signature in declaredSignatures) {
            // Note: It might happen that there are clashing signatures coming from different libraries.
            // At the moment, we will just overwrite the first occurrence with the next one(s).
            // However, this should be fixed in the appropriate way once we have a design decision for KT-82172.
            // TODO(KT-82172): Handle clashing signatures here in the proper way.
            declaredSignatureToNode[signature] = node
        }

        nodeToImportedSignatures[node] = importedSignatures

        return true // The indices were populated now.
    }

    private enum class State {
        SCHEDULED_FOR_PROCESSING,
        ALREADY_PROCESSED,
    }
}

private class KlibDAGNodeImpl(override val library: KotlinLibrary) : KlibDAGNode {
    val targets = hashSetOf<KlibDAGNodeImpl>()

    override val directDependencies: Set<KotlinLibrary> by LockBasedStorageManager.NO_LOCKS.createLazyValue {
        targets.mapTo(hashSetOf()) { it.library }
    }

    override val allDependencies: Set<KotlinLibrary> by LockBasedStorageManager.NO_LOCKS.createLazyValue(
        computable = {
            buildSet {
                addAll(directDependencies)
                targets.flatMapTo(this, KlibDAGNode::allDependencies)
            }
        },
        onRecursiveCall = {
            throw KlibDAGCyclicDependencyException()
        }
    )
}
