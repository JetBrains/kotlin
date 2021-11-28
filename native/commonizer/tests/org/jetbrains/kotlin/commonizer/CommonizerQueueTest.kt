/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cir.CirRoot
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test
import kotlin.test.assertEquals

class CommonizerQueueTest {

    data class CommonizerInvocation(val inputs: TargetDependent<CirTreeRoot?>, val output: SharedCommonizerTarget)

    @Test
    fun `test retained targets`() {
        val queue = CommonizerQueue(
            storageManager = LockBasedStorageManager.NO_LOCKS,
            outputTargets = setOf("(a, b)", "(a, b, c)").map(::parseCommonizerTarget).map { it as SharedCommonizerTarget }.toSet(),
            deserializers = EagerTargetDependent(
                setOf("a", "b", "c").map(::parseCommonizerTarget)
            ) { CommonizerQueue.Deserializer { null } },
            commonizer = { _, _ -> null },
            serializer = { _, _ -> },
        )

        assertEquals(
            setOf("a", "b", "c"), queue.retainedDeserializedTargets.map { it.identityString }.toSet(),
            "Expected all targets a, b, c to be retained in the beginning"
        )

        assertEquals(
            setOf("(a, b)", "(a, b, c)"), queue.retainedCommonizedTargets.map { it.identityString }.toSet(),
            "Expected all output targets to be retained in the beginning"
        )

        assertEquals(
            setOf("(a, b)", "(a, b, c)"), queue.retainedTargetDependencies.keys.map { it.identityString }.toSet(),
            "Expected all output targets to declare its dependencies"
        )

        queue.invokeTarget(parseCommonizerTarget("(a, b)") as SharedCommonizerTarget)

        assertEquals(
            setOf("c"), queue.retainedDeserializedTargets.map { it.identityString }.toSet(),
            "Expected only target 'c' to be retained after commonizing (a, b)"
        )

        assertEquals(
            setOf("(a, b)", "(a, b, c)"), queue.retainedCommonizedTargets.map { it.identityString }.toSet(),
            "Expected all output targets to be retained after commonizing (a, b)"
        )

        assertEquals(
            setOf("(a, b, c)"), queue.retainedTargetDependencies.keys.map { it.identityString }.toSet(),
            "Expected remaining targets to declare its dependencies"
        )

        assertEquals(queue.pendingOutputTargets, queue.retainedTargetDependencies.keys)

        queue.invokeTarget(parseCommonizerTarget("(a, b, c)") as SharedCommonizerTarget)

        assertEquals(
            emptySet(), queue.retainedDeserializedTargets,
            "Expected no retained deserialized targets"
        )

        assertEquals(
            emptySet(), queue.retainedCommonizedTargets,
            "Expected no retained commonized targets"
        )

        assertEquals(
            emptySet(), queue.retainedTargetDependencies.keys,
            "Expected no retained target dependencies"
        )

        assertEquals(queue.pendingOutputTargets, queue.retainedTargetDependencies.keys)

        queue.invokeAll()
    }

    @Test
    fun `test commonizer being called`() {
        val commonizerInvocations = mutableListOf<CommonizerInvocation>()
        val storageManager = LockBasedStorageManager.NO_LOCKS
        val providedTargets = setOf("a", "b", "c").map(::parseCommonizerTarget).toSet()
        val abOutputTarget = parseCommonizerTarget("(a, b)") as SharedCommonizerTarget
        val abcOutputTarget = parseCommonizerTarget("(a, b, c)") as SharedCommonizerTarget
        val outputTargets = setOf(abOutputTarget, abcOutputTarget)

        val queue = CommonizerQueue(
            storageManager = storageManager,
            outputTargets = outputTargets,
            deserializers = EagerTargetDependent(providedTargets) { CommonizerQueue.Deserializer { null } },
            commonizer = { inputs, output ->
                commonizerInvocations.add(CommonizerInvocation(inputs, output))
                CirRootNode(
                    CirProvidedClassifiers.EMPTY, CommonizedGroup(0), storageManager.createNullableLazyValue { CirRoot.create(output) }
                )
            },
            serializer = { _, _ -> },
        )

        queue.invokeAll()

        assertEquals(
            2, commonizerInvocations.size,
            "Expected 2 commonizer invocations"
        )

        assertEquals(
            outputTargets, commonizerInvocations.map { it.output }.toSet(),
            "Expected specified output targets to be invoked"
        )

        val abInvocation = commonizerInvocations.single { it.output == abOutputTarget }
        assertEquals(
            selectInputTargets(providedTargets + outputTargets, abOutputTarget),
            abInvocation.inputs.targets.toSet(),
            "Expected commonizer being invoked with selected targets for abInvocation"
        )

        val abcInvocation = commonizerInvocations.single { it.output == abcOutputTarget }
        assertEquals(
            selectInputTargets(providedTargets + outputTargets, abcOutputTarget),
            abcInvocation.inputs.targets.toSet(),
            "Expected commonizer being invoked with selected targets for abcInvocation"
        )
    }

    @Test
    fun `test diamond output targets`() {
        val commonizerInvocations = mutableListOf<CommonizerInvocation>()

        val queue = CommonizerQueue(
            storageManager = LockBasedStorageManager.NO_LOCKS,
            outputTargets = setOf("(a, b)", "(b, c)", "(a, b, c)")
                .map(::parseCommonizerTarget).map { it as SharedCommonizerTarget }
                .toSet(),
            deserializers = EagerTargetDependent(
                setOf("a", "b", "c", "d").map(::parseCommonizerTarget)
            ) { CommonizerQueue.Deserializer { null } },
            commonizer = { inputs, output ->
                commonizerInvocations.add(CommonizerInvocation(inputs, output))
                CirRootNode(
                    CirProvidedClassifiers.EMPTY,
                    CommonizedGroup(0), LockBasedStorageManager.NO_LOCKS.createNullableLazyValue { CirRoot.create(output) }
                )
            },
            serializer = { _, _ -> },
        )

        queue.invokeAll()
        assertEquals(emptySet(), queue.pendingOutputTargets, "Expected empty pendingOutputTargets")
        assertEquals(emptySet(), queue.retainedCommonizedTargets, "Expected no retained commonized targets")
        assertEquals(emptySet(), queue.retainedDeserializedTargets, "Expected no retained deserialized targets")
        assertEquals(emptySet(), queue.retainedTargetDependencies.entries, "Expected no retained dependencies")
        assertEquals(
            listOf(
                listOf(parseCommonizerTarget("a"), parseCommonizerTarget("b")),
                listOf(parseCommonizerTarget("b"), parseCommonizerTarget("c")),
                listOf(parseCommonizerTarget("(a, b)"), parseCommonizerTarget("(b, c)"))
            ), commonizerInvocations.map { it.inputs.targets }
        )
    }
}
