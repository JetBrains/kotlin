package org.jetbrains.dokka.base.transformers.documentables

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.plus
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer


class ExtensionExtractorTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule = runBlocking(Dispatchers.Default) {
        val channel = Channel<Pair<DRI, Callable>>(10)
        launch {
            coroutineScope {
                original.packages.forEach { launch { collectExtensions(it, channel) } }
            }
            channel.close()
        }
        val extensionMap = channel.consumeAsFlow().toList().toMultiMap()

        val newPackages = original.packages.map { async { it.addExtensionInformation(extensionMap) } }
        original.copy(packages = newPackages.awaitAll())
    }
}

private suspend fun <T : Documentable> T.addExtensionInformation(
    extensionMap: Map<DRI, List<Callable>>
): T = coroutineScope {
    val newClasslikes = (this@addExtensionInformation as? WithScope)
        ?.classlikes
        ?.map { async { it.addExtensionInformation(extensionMap) } }
        .orEmpty()

    @Suppress("UNCHECKED_CAST")
    when (this@addExtensionInformation) {
        is DPackage -> {
            val newTypealiases = typealiases.map { async { it.addExtensionInformation(extensionMap) } }
            copy(classlikes = newClasslikes.awaitAll(), typealiases = newTypealiases.awaitAll())
        }
        is DClass -> copy(classlikes = newClasslikes.awaitAll(), extra = extra + extensionMap.find(dri))
        is DEnum -> copy(classlikes = newClasslikes.awaitAll(), extra = extra + extensionMap.find(dri))
        is DInterface -> copy(classlikes = newClasslikes.awaitAll(), extra = extra + extensionMap.find(dri))
        is DObject -> copy(classlikes = newClasslikes.awaitAll(), extra = extra + extensionMap.find(dri))
        is DAnnotation -> copy(classlikes = newClasslikes.awaitAll(), extra = extra + extensionMap.find(dri))
        is DTypeAlias -> copy(extra = extra + extensionMap.find(dri))
        else -> throw IllegalStateException(
            "${this@addExtensionInformation::class.simpleName} is not expected to have extensions"
        )
    } as T
}

private fun Map<DRI, List<Callable>>.find(dri: DRI) = get(dri)?.toSet()?.let(::CallableExtensions)

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
                .flatMap(Callable::asPairsWithReceiverDRIs)
                .forEach { channel.send(it) }
        }
    }
}


private fun Callable.asPairsWithReceiverDRIs(): Sequence<Pair<DRI, Callable>> =
    receiver?.type?.let(::findReceiverDRIs).orEmpty().map { it to this }

// In normal cases we return at max one DRI, but sometimes receiver type can be bound by more than one type constructor
// for example `fun <T> T.example() where T: A, T: B` is extension of both types A and B
// Note: in some cases returning empty sequence doesn't mean that we cannot determine the DRI but only that we don't
// care about it since there is nowhere to put documentation of given extension.
private fun Callable.findReceiverDRIs(bound: Bound): Sequence<DRI> = when (bound) {
    is Nullable -> findReceiverDRIs(bound.inner)
    is TypeParameter ->
        if (this is DFunction && bound.dri == this.dri)
            generics.find { it.name == bound.name }?.bounds?.asSequence()?.flatMap(::findReceiverDRIs).orEmpty()
        else
            emptySequence()
    is TypeConstructor -> sequenceOf(bound.dri)
    is PrimitiveJavaType -> emptySequence()
    is Void -> emptySequence()
    is JavaObject -> sequenceOf(DriOfAny)
    is Dynamic -> sequenceOf(DriOfAny)
    is UnresolvedBound -> emptySequence()
}

private fun <T, U> Iterable<Pair<T, U>>.toMultiMap(): Map<T, List<U>> =
    groupBy(Pair<T, *>::first, Pair<*, U>::second)

data class CallableExtensions(val extensions: Set<Callable>) : ExtraProperty<Documentable> {
    companion object Key : ExtraProperty.Key<Documentable, CallableExtensions> {
        override fun mergeStrategyFor(left: CallableExtensions, right: CallableExtensions) =
            MergeStrategy.Replace(CallableExtensions(left.extensions + right.extensions))
    }

    override val key = Key
}

//TODO IMPORTANT remove this terrible hack after updating to 1.4-M3
fun <T : Any> ReceiveChannel<T>.consumeAsFlow(): Flow<T> = flow {
    try {
        while (true) {
            emit(receive())
        }
    } catch (_: ClosedReceiveChannelException) {
        // cool and good
    }
}.flowOn(Dispatchers.Default)