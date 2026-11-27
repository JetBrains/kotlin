/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.documentation.KdElementId
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

internal class ExtensionExtractorTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule = runBlocking(Dispatchers.Default) {
        val classGraph = async {
            if (!context.configuration.suppressInheritedMembers)
                context.plugin<InternalKotlinAnalysisPlugin>().querySingle { fullClassHierarchyBuilder }.build(original)
            else
                emptyMap()
        }

        val channel = Channel<Pair<KdElementId, Callable>>(10)
        launch {
            original.packages.parallelForEach { collectExtensions(it, channel) }
            channel.close()
        }
        val extensionMap = channel.toList().toMultiMap()

        val newPackages = original.packages.parallelMap { it.addExtensionInformation(classGraph.await(), extensionMap) }
        original.copy(packages = newPackages)
    }

    private suspend fun <T : Documentable> T.addExtensionInformation(
        classGraph: SourceSetDependent<Map<KdElementId, List<KdElementId>>>,
        extensionMap: Map<KdElementId, List<Callable>>
    ): T = coroutineScope {
        val newClasslikes = (this@addExtensionInformation as? WithScope)
            ?.classlikes
            ?.map { async { it.addExtensionInformation(classGraph, extensionMap) } }
            .orEmpty()
        val newTypealiases = (this@addExtensionInformation as? WithTypealiases)
            ?.typealiases
            ?.map { async { it.addExtensionInformation(classGraph, extensionMap) } }
            .orEmpty()

        val result = when (this@addExtensionInformation) {
            is DPackage -> copy(
                classlikes = newClasslikes.awaitAll(),
                typealiases = newTypealiases.awaitAll(),
            )

            is DClass -> copy(
                classlikes = newClasslikes.awaitAll(),
                typealiases = newTypealiases.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DEnum -> copy(
                classlikes = newClasslikes.awaitAll(),
                typealiases = newTypealiases.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DInterface -> copy(
                classlikes = newClasslikes.awaitAll(),
                typealiases = newTypealiases.awaitAll(),
                extra = extra + findExtensions(classGraph, extensionMap)
            )

            is DObject -> copy(
                classlikes = newClasslikes.awaitAll(),
                typealiases = newTypealiases.awaitAll(),
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
        }
        @Suppress("UNCHECKED_CAST")
        result as T
    }

    private suspend fun collectExtensions(
        documentable: Documentable,
        channel: SendChannel<Pair<KdElementId, Callable>>
    ): Unit = coroutineScope {
        if (documentable is WithScope) {
            documentable.classlikes.forEach {
                launch { collectExtensions(it, channel) }
            }

            if (documentable is DObject || documentable is DPackage) {
                (documentable.properties.asSequence() + documentable.functions.asSequence())
                    .flatMap { it.asPairsWithReceiverDeclarationIds() }
                    .forEach { channel.send(it) }
            }
        }
    }

    private fun <T : Documentable> T.findExtensions(
        classGraph: SourceSetDependent<Map<KdElementId, List<KdElementId>>>,
        extensionMap: Map<KdElementId, List<Callable>>
    ): CallableExtensions? {
        val resultSet = mutableSetOf<Callable>()

        fun collectFrom(element: KdElementId, isSupertype: Boolean) {
            extensionMap[element]?.forEach { extension ->
                // A companion extension belongs to the companion scope of its receiver type only.
                // Such a scope is not inherited: it cannot be accessed through the name of a subtype
                // (see KEEP-0449 §2.5), so companion extensions of supertypes are not collected here.
                if (!isSupertype || !extension.isCompanionExtension()) resultSet.add(extension)
            }
            sourceSets.forEach { sourceSet ->
                classGraph[sourceSet]?.get(element)?.forEach { collectFrom(it, isSupertype = true) }
            }
        }
        collectFrom(declarationId, isSupertype = false)

        return if (resultSet.isEmpty()) null else CallableExtensions(resultSet)
    }

    @OptIn(ExperimentalDokkaApi::class)
    private fun Callable.isCompanionExtension(): Boolean = when (this) {
        is DFunction -> extra[IsCompanion] != null
        is DProperty -> extra[IsCompanion] != null
        else -> false
    }

    private fun Callable.asPairsWithReceiverDeclarationIds(): Sequence<Pair<KdElementId, Callable>> =
        receiver?.type?.let { findReceiverDeclarationIds(it) }.orEmpty().map { it to this }

    // In normal cases we return at max one declaration ID, but sometimes the receiver type can be bound by
    // more than one type constructor, for example `fun <T> T.example() where T: A, T: B` is extension of
    // both types A and B; another one is `typealias A = B`.
    // Note: in some cases returning an empty sequence doesn't mean that we cannot determine the ID but only
    // that we don't care about it since there is nowhere to put documentation of given extension.
    private fun Callable.findReceiverDeclarationIds(bound: Bound): Sequence<KdElementId> = when (bound) {
        is Nullable -> findReceiverDeclarationIds(bound.inner)
        is DefinitelyNonNullable -> findReceiverDeclarationIds(bound.inner)
        // NB: the old DRI-based code compared DRIs that could never match, so a type-parameter receiver
        // never resolves to any class ID.
        is TypeParameter -> emptySequence()

        is TypeConstructor -> sequenceOf(bound.declarationId)
        is PrimitiveJavaType -> emptySequence()
        is Void -> emptySequence()
        is JavaObject -> sequenceOf(KdElementId("kotlin/Any"))
        is Dynamic -> sequenceOf(KdElementId("kotlin/Any"))
        is UnresolvedBound -> emptySequence()
        is TypeAliased -> findReceiverDeclarationIds(bound.typeAlias) + findReceiverDeclarationIds(bound.inner)
    }

    private fun <T, U> Iterable<Pair<T, U>>.toMultiMap(): Map<T, List<U>> =
        groupBy(Pair<T, *>::first, Pair<*, U>::second)
}

internal data class CallableExtensions(val extensions: Set<Callable>) : ExtraProperty<Documentable> {
    companion object Key : ExtraProperty.Key<Documentable, CallableExtensions> {
        override fun mergeStrategyFor(
            left: CallableExtensions,
            right: CallableExtensions
        ): MergeStrategy<Documentable> =
            MergeStrategy.Replace(CallableExtensions(left.extensions + right.extensions))
    }

    override val key: Key = Key
}
