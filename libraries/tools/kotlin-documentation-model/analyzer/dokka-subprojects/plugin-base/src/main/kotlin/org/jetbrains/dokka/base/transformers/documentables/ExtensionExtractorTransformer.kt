/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.plus
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin

public class ExtensionExtractorTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule = runBlocking(Dispatchers.Default) {
        val classGraph = async {
            if (!context.configuration.suppressInheritedMembers)
                context.plugin<InternalKotlinAnalysisPlugin>().querySingle { fullClassHierarchyBuilder }.build(original)
            else
                emptyMap()
        }

        val channel = Channel<Pair<DRI, Callable>>(10)
        launch {
            original.packages.parallelForEach { collectExtensions(it, channel) }
            channel.close()
        }
        val extensionMap = channel.toList().toMultiMap()

        val newPackages = original.packages.parallelMap { it.addExtensionInformation(classGraph.await(), extensionMap) }
        original.copy(packages = newPackages)
    }

    private suspend fun <T : Documentable> T.addExtensionInformation(
        classGraph: SourceSetDependent<Map<DRI, List<DRI>>>,
        extensionMap: Map<DRI, List<Callable>>
    ): T = coroutineScope {
        val newClasslikes = (this@addExtensionInformation as? WithScope)
            ?.classlikes
            ?.map { async { it.addExtensionInformation(classGraph, extensionMap) } }
            .orEmpty()

        @Suppress("UNCHECKED_CAST")
        when (this@addExtensionInformation) {
            is DPackage -> {
                val newTypealiases = typealiases.map { async { it.addExtensionInformation(classGraph, extensionMap) } }
                copy(classlikes = newClasslikes.awaitAll(), typealiases = newTypealiases.awaitAll())
            }

            is DClass -> copy(
                classlikes = newClasslikes.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DEnum -> copy(
                classlikes = newClasslikes.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DInterface -> copy(
                classlikes = newClasslikes.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DObject -> copy(
                classlikes = newClasslikes.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DAnnotation -> copy(
                classlikes = newClasslikes.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DTypeAlias -> copy(extra = extra + findExtensions(classGraph, extensionMap))
            else -> throw IllegalStateException(
                "${this@addExtensionInformation::class.simpleName} is not expected to have extensions"
            )
        } as T
    }

    private suspend fun collectExtensions(
        documentable: Documentable,
        channel: SendChannel<Pair<DRI, Callable>>
    ): Unit = coroutineScope {
        if (documentable is WithScope) {
            documentable.classlikes.forEach {
                launch { collectExtensions(it, channel) }
            }

            if (documentable is DObject || documentable is DPackage) {
                (documentable.properties.asSequence() + documentable.functions.asSequence())
                    .flatMap { it.asPairsWithReceiverDRIs() }
                    .forEach { channel.send(it) }
            }
        }
    }

    private fun <T : Documentable> T.findExtensions(
        classGraph: SourceSetDependent<Map<DRI, List<DRI>>>,
        extensionMap: Map<DRI, List<Callable>>
    ): CallableExtensions? {
        val resultSet = mutableSetOf<Callable>()

        fun collectFrom(element: DRI) {
            extensionMap[element]?.let { resultSet.addAll(it) }
            sourceSets.forEach { sourceSet -> classGraph[sourceSet]?.get(element)?.forEach { collectFrom(it) } }
        }
        collectFrom(dri)

        return if (resultSet.isEmpty()) null else CallableExtensions(resultSet)
    }

    private fun Callable.asPairsWithReceiverDRIs(): Sequence<Pair<DRI, Callable>> =
        receiver?.type?.let { findReceiverDRIs(it) }.orEmpty().map { it to this }

    // In normal cases we return at max one DRI, but sometimes receiver type can be bound by more than one type constructor
    // for example `fun <T> T.example() where T: A, T: B` is extension of both types A and B
    // another one `typealias A = B`
    // Note: in some cases returning empty sequence doesn't mean that we cannot determine the DRI but only that we don't
    // care about it since there is nowhere to put documentation of given extension.
    private fun Callable.findReceiverDRIs(bound: Bound): Sequence<DRI> = when (bound) {
        is Nullable -> findReceiverDRIs(bound.inner)
        is DefinitelyNonNullable -> findReceiverDRIs(bound.inner)
        is TypeParameter ->
            if (this is DFunction && bound.dri == this.dri)
                generics.find { it.name == bound.name }?.bounds?.asSequence()?.flatMap { findReceiverDRIs(it) }.orEmpty()
            else
                emptySequence()

        is TypeConstructor -> sequenceOf(bound.dri)
        is PrimitiveJavaType -> emptySequence()
        is Void -> emptySequence()
        is JavaObject -> sequenceOf(DriOfAny)
        is Dynamic -> sequenceOf(DriOfAny)
        is UnresolvedBound -> emptySequence()
        is TypeAliased -> findReceiverDRIs(bound.typeAlias) + findReceiverDRIs(bound.inner)
    }

    private fun <T, U> Iterable<Pair<T, U>>.toMultiMap(): Map<T, List<U>> =
        groupBy(Pair<T, *>::first, Pair<*, U>::second)
}

public data class CallableExtensions(val extensions: Set<Callable>) : ExtraProperty<Documentable> {
    public companion object Key : ExtraProperty.Key<Documentable, CallableExtensions> {
        override fun mergeStrategyFor(left: CallableExtensions, right: CallableExtensions): MergeStrategy<Documentable> =
            MergeStrategy.Replace(CallableExtensions(left.extensions + right.extensions))
    }

    override val key: Key = Key
}
