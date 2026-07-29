/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractorFromRegularKlib
import org.jetbrains.kotlin.backend.konan.serialization.IdSignaturesExtractorFromCInteropKlib
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.ir
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.getValue
import kotlin.io.path.pathString

interface KlibDAGNode {
    val library: KotlinLibrary
    val directDependencies: Set<KlibDAGNode>
    val allDependencies: Set<KlibDAGNode>
}

typealias KlibDAG = Map<KotlinLibrary, KlibDAGNode>

class KlibDAGCyclicDependencyException : Exception("Recursive dependency detected while computing DAG of KLIB dependencies")

object KlibDAGBuilder {
    fun build(libraries: Collection<KotlinLibrary>): KlibDAG {
        checkNoDuplicatedLibraries(libraries)

        val dag: Map<KotlinLibrary, KlibDAGNodeImpl> = libraries.associateWith(::KlibDAGNodeImpl)

        // Index: declared signature -> KLIB.
        val declaredSignatureToNode: MutableMap<IdSignature, KlibDAGNodeImpl> = hashMapOf()

        // Index: KLIB -> imported signatures.
        val nodeToImportedSignatures: MutableMap<KlibDAGNodeImpl, Set<IdSignature>> = hashMapOf()

        val [stdlib: List<KlibDAGNodeImpl>, others: List<KlibDAGNodeImpl>] = dag.values.partition { it.library.isNativeStdlib }

        // Fill in indices.
        for (node in others) {
            // Optimization: Stdlib is a dependency for each library.
            node.directDependencies.addAll(stdlib)

            val [declaredSignatures, importedSignatures] = node.library.getSignatureExtractor().extractOnlyTopLevelPublicSignatures()

            for (signature in declaredSignatures) {
                declaredSignatureToNode[signature] = node
            }

            nodeToImportedSignatures[node] = importedSignatures
        }

        // Build the DAG.
        for ([node, importedSignatures] in nodeToImportedSignatures) {
            for (importedSignature in importedSignatures) {
                val dependency: KlibDAGNodeImpl? = declaredSignatureToNode[importedSignature]
                if (dependency != null) {
                    node.directDependencies.add(dependency)
                }
            }
        }

        return dag
    }

    private fun checkNoDuplicatedLibraries(libraries: Collection<KotlinLibrary>) {
        val duplicatedLibraries: Map<String, List<KotlinLibrary>> = libraries.groupBy { it.path.canonicalPathString() }.filterValues { it.size > 1 }
        if (duplicatedLibraries.isEmpty()) return

        val errorMessage = buildString {
            duplicatedLibraries.values.forEach { duplicates ->
                append("Duplicated libraries found: ")
                duplicates.joinTo(this, postfix = "\n") { it.path.pathString }
            }
        }

        error(errorMessage)
    }

    private fun KotlinLibrary.getSignatureExtractor(): IdSignaturesExtractor = when {
        isCInteropLibrary() -> IdSignaturesExtractorFromCInteropKlib(this)
        ir != null -> IdSignaturesExtractorFromRegularKlib(this)
        else -> error("This library does not have IR and is not a C-interop library: $path")
    }
}

private class KlibDAGNodeImpl(override val library: KotlinLibrary) : KlibDAGNode {
    override val directDependencies = hashSetOf<KlibDAGNodeImpl>()

    override val allDependencies: Set<KlibDAGNode> by LockBasedStorageManager.NO_LOCKS.createLazyValue(
        computable = {
            buildSet {
                addAll(directDependencies)
                directDependencies.flatMapTo(this, KlibDAGNode::allDependencies)
            }
        },
        onRecursiveCall = {
            throw KlibDAGCyclicDependencyException()
        }
    )
}
