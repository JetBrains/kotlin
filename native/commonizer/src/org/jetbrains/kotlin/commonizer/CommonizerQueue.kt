/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.tree.assembleCirTree
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

private typealias OutputCommonizerTarget = SharedCommonizerTarget
private typealias InputCommonizerTarget = CommonizerTarget

internal fun CommonizerQueue(parameters: CommonizerParameters): CommonizerQueue {
    return CommonizerQueue(
        storageManager = parameters.storageManager,
        outputTargets = parameters.outputTargets,
        deserializers = parameters.targetProviders.mapTargets { target ->
            CommonizerQueue.Deserializer { deserializeTarget(parameters, target) }
        },
        commonizer = { inputs, output -> commonizeTarget(parameters, inputs, output) },
        serializer = { declarations, outputTarget -> serializeTarget(parameters, declarations, outputTarget) },
        inputTargetsSelector = DefaultInputTargetsSelector
    )
}

internal class CommonizerQueue(
    private val storageManager: StorageManager,
    private val outputTargets: Set<OutputCommonizerTarget>,
    private val deserializers: TargetDependent<Deserializer>,
    private val commonizer: Commonizer,
    private val serializer: Serializer,
    private val inputTargetsSelector: InputTargetsSelector
) {

    fun interface Deserializer {
        operator fun invoke(): CirTreeRoot?
    }

    fun interface Commonizer {
        operator fun invoke(inputs: TargetDependent<CirTreeRoot?>, output: SharedCommonizerTarget): CirRootNode?
    }

    fun interface Serializer {
        operator fun invoke(declarations: CirRootNode, outputTarget: OutputCommonizerTarget)
    }

    /**
     * Targets that can just be deserialized and do not need to be commonized.
     * All leaf targets are expected to be provided.
     * Previously commonized targets can also be provided
     */
    private val deserializedTargets: MutableMap<InputCommonizerTarget, NullableLazyValue<CirTreeRoot>> =
        deserializers.toMap().mapValuesTo(mutableMapOf()) { (_, deserializer) ->
            storageManager.createNullableLazyValue { deserializer() }
        }

    /**
     * Targets that created using commonization.
     * The roots are lazy and will be removed as no further pending target requires it's input
     */
    private val commonizedTargets: MutableMap<OutputCommonizerTarget, NullableLazyValue<CirTreeRoot>> = mutableMapOf()

    /**
     * Represents dependency relationships between input and output targets.
     * Dependencies will be removed if the target was commonized.
     */
    private val targetDependencies: MutableMap<OutputCommonizerTarget, Set<InputCommonizerTarget>> = mutableMapOf()

    val retainedDeserializedTargets: Set<InputCommonizerTarget> get() = deserializedTargets.keys

    val retainedCommonizedTargets: Set<OutputCommonizerTarget> get() = commonizedTargets.keys

    val retainedTargetDependencies: Map<OutputCommonizerTarget, Set<InputCommonizerTarget>> get() = targetDependencies.toMap()

    val pendingOutputTargets: Set<CommonizerTarget> get() = targetDependencies.keys

    /**
     * Runs all tasks/targets in this queue
     */
    fun invokeAll() {
        outputTargets.forEach { outputTarget -> invokeTarget(outputTarget) }
        assert(deserializedTargets.isEmpty()) { "Expected 'deserializedTargets' to be empty. Found ${deserializedTargets.keys}" }
        assert(commonizedTargets.isEmpty()) { "Expected 'commonizedTargets' to be empty. Found ${commonizedTargets.keys}" }
        assert(targetDependencies.isEmpty()) { "Expected 'targetDependencies' to be empty. Found $targetDependencies" }
    }

    fun invokeTarget(outputTarget: OutputCommonizerTarget) {
        commonizedTargets[outputTarget]?.invoke()
    }

    private fun enqueue(outputTarget: OutputCommonizerTarget) {
        registerTargetDependencies(outputTarget)

        commonizedTargets[outputTarget] = storageManager.createNullableLazyValue {
            commonize(outputTarget)
        }
    }

    private fun commonize(target: SharedCommonizerTarget): CirTreeRoot? {
        val inputTargets = targetDependencies.getValue(target)

        val inputDeclarations = EagerTargetDependent(inputTargets) { inputTarget ->
            (deserializedTargets[inputTarget] ?: commonizedTargets[inputTarget]
            ?: throw IllegalStateException("Missing inputTarget $inputTarget")).invoke()
        }

        return commonizer(inputDeclarations, target)
            .also { removeTargetDependencies(target) }
            ?.also { commonizedDeclarations -> serializer(commonizedDeclarations, target) }
            ?.assembleCirTree()

    }

    private fun registerTargetDependencies(outputTarget: OutputCommonizerTarget) {
        targetDependencies[outputTarget] = inputTargetsSelector(outputTargets + deserializers.targets, outputTarget)
    }

    private fun removeTargetDependencies(target: OutputCommonizerTarget) {
        targetDependencies.remove(target) ?: return
        val referencedDependencyTargets = targetDependencies.values.flatten().toSet()

        // Release all commonized targets that are not pending anymore (are already invoked)
        //  and that are not listed as input target dependency for any further commonization
        //  Release all commonized targets that no one intends to use any further.
        commonizedTargets.keys
            .filter { it !in referencedDependencyTargets && it !in pendingOutputTargets }
            .forEach(commonizedTargets::remove)

        // Release all deserialized targets that are not referenced as any further dependency anymore.
        //  Release all deserialized targets that no one intends to use any further
        deserializedTargets.keys
            .filter { it !in referencedDependencyTargets }
            .forEach(deserializedTargets::remove)
    }

    init {
        outputTargets.forEach(this::enqueue)
    }
}