/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment

class ChunkedKlibModuleFragmentWriteStrategy(
    private val topLevelClassifierDeclarationsPerFile: Int = 64,
    private val topLevelCallableDeclarationsPerFile: Int = 128
) : KlibModuleFragmentWriteStrategy {
    init {
        check(topLevelClassifierDeclarationsPerFile > 0) {
            "Invalid top level classifiers per file: $topLevelClassifierDeclarationsPerFile"
        }
        check(topLevelCallableDeclarationsPerFile > 0) {
            "Invalid top level callables per file: $topLevelCallableDeclarationsPerFile"
        }
    }

    override fun processPackageParts(parts: List<KmModuleFragment>): List<KmModuleFragment> {
        if (parts.isEmpty()) return emptyList()

        val fqName = checkNotNull(parts.first().fqName) { "KmModuleFragment should have a not-null fqName!" }

        val chunks = mutableListOf<KmModuleFragment>()
        var lastChunk: KmModuleFragment? = null

        fun createNewChunk(): KmModuleFragment = KmModuleFragment().also { chunk ->
            chunk.fqName = fqName
            chunks += chunk
            lastChunk = chunk
        }

        fun getLastChunkOrCreateNew(): KmModuleFragment = lastChunk ?: createNewChunk()

        fun KmModuleFragment.getExistingPkgOrCreateNew(): KmPackage = pkg ?: KmPackage().also { pkg ->
            pkg.fqName = this.fqName
            this.pkg = pkg
        }

        parts.asSequence().flatMap(ClassifierBucket.Companion::createBuckets).forEach { bucket ->
            val currentChunk = getLastChunkOrCreateNew()
            val currentChunkSize = currentChunk.classes.size + (currentChunk.pkg?.typeAliases?.size ?: 0)

            val chunkToAddClassifiers =
                if (currentChunkSize == 0 || currentChunkSize + bucket.size <= topLevelClassifierDeclarationsPerFile)
                    currentChunk
                else
                    createNewChunk()

            when (bucket) {
                is ClassifierBucket.Classes -> {
                    chunkToAddClassifiers.classes += bucket.classes
                    chunkToAddClassifiers.className += bucket.classNames
                }

                is ClassifierBucket.TypeAlias -> {
                    chunkToAddClassifiers.getExistingPkgOrCreateNew().typeAliases += bucket.typeAlias
                }
            }
        }

        parts.asSequence().flatMap { it.pkg?.let { pkg -> pkg.functions.asSequence() + pkg.properties } ?: emptySequence() }
            .chunked(topLevelCallableDeclarationsPerFile)
            .forEach { chunkedCallables ->
                val pkgToAddCallables = createNewChunk().getExistingPkgOrCreateNew()
                chunkedCallables.forEach { callable ->
                    when (callable) {
                        is KmFunction -> pkgToAddCallables.functions += callable
                        is KmProperty -> pkgToAddCallables.properties += callable
                        else -> error("Unexpected callable type: $callable")
                    }
                }
            }

        return chunks.ifEmpty {
            // We still need to emit empty packages because they may represent parts of package declarations
            // (e.g. platform.* in C-interop KLIBs).
            parts
        }
    }
}

private sealed interface ClassifierBucket {
    val size: Int

    class Classes private constructor(val classes: List<KmClass>) : ClassifierBucket {
        override val size get() = classes.size
        val classNames get() = classes.map(KmClass::name)

        companion object {
            /**
             * Split the sequence of [KmClass]es into multiple [Classes] so that
             * classes with the same top-level name go to the same bucket.
             */
            fun createBuckets(classes: List<KmClass>): Sequence<Classes> = when (classes.size) {
                0 -> emptySequence()
                1 -> sequenceOf(Classes(classes))
                else -> {
                    val groupedByTopLevelClassNames = linkedMapOf<ClassName, MutableList<KmClass>>()

                    for (clazz in classes) {
                        val className = clazz.name
                        val topLevelClassName = if (!className.isLocalClassName()) className.substringBefore('.') else className
                        groupedByTopLevelClassNames.computeIfAbsent(topLevelClassName) { mutableListOf() } += clazz
                    }

                    groupedByTopLevelClassNames.values.asSequence().map(::Classes)
                }
            }
        }
    }

    class TypeAlias(val typeAlias: KmTypeAlias) : ClassifierBucket {
        override val size get() = 1

        companion object {
            /** Wrap the sequence of [KmTypeAlias]es as the sequence of individual [ClassifierBucket.TypeAlias]es. */
            fun createBuckets(typeAliases: List<KmTypeAlias>?): Sequence<TypeAlias> =
                if (typeAliases.isNullOrEmpty()) emptySequence() else typeAliases.asSequence().map(::TypeAlias)
        }
    }

    companion object {
        fun createBuckets(fragment: KmModuleFragment): Sequence<ClassifierBucket> =
            Classes.createBuckets(fragment.classes) + TypeAlias.createBuckets(fragment.pkg?.typeAliases)
    }
}
