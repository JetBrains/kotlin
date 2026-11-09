/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.analyzer.internal.checkElementIds
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class ElementIdSanityCheckerTest {
    private val id = KdElementId("member")
    private val target = KdElementId("target")

    @Test
    fun `references can resolve to embedded elements`() {
        bundle(mapOf(id to KdInheritedMember(target)), mapOf(target to KdInheritedMember(target))).checkElementIds()
    }

    @Test
    fun `missing references include their location`() {
        val failure = assertFailsWith<IllegalStateException> {
            bundle(mapOf(id to KdInheritedMember(target))).checkElementIds()
        }
        assertContains(failure.message.orEmpty(), "main.elements[member].originalId: 'target'")
    }

    @Test
    fun `references must exist in the referenced fragment`() {
        val bundle = bundle(mapOf(id to KdInheritedMember(id))).copy(
            expectActualMappings = listOf(KdExpectActualMapping(mapOf("other" to id), emptyMap())),
        )
        assertFailsWith<IllegalStateException> { bundle.checkElementIds() }
    }

    @Test
    fun `every expect and actual ID must exist in its specified fragment`() {
        val bundle = KdBundle(
            "test",
            listOf(
                KdFragment("commonMain", mapOf(id to KdInheritedMember(id)), emptyMap()),
                KdFragment("jvmMain", mapOf(target to KdInheritedMember(target)), emptyMap()),
            ),
            listOf(
                KdExpectActualMapping(
                    expectIds = mapOf("commonMain" to id),
                    actualIds = mapOf("jvmMain" to target),
                ),
            ),
        )
        bundle.checkElementIds()

        val invalid = bundle.copy(
            expectActualMappings = listOf(
                KdExpectActualMapping(
                    expectIds = mapOf("commonMain" to id, "jvmMain" to id),
                    actualIds = mapOf("jvmMain" to target, "commonMain" to target),
                ),
            ),
        )
        val failure = assertFailsWith<IllegalStateException> { invalid.checkElementIds() }
        assertContains(failure.message.orEmpty(), "expectActualMappings[0].expectIds[jvmMain]: 'member'")
        assertContains(failure.message.orEmpty(), "expectActualMappings[0].actualIds[commonMain]: 'target'")
    }

    @Test
    fun `expect and actual IDs absent from the entire bundle are reported`() {
        val invalid = bundle(emptyMap()).copy(
            expectActualMappings = listOf(
                KdExpectActualMapping(mapOf("main" to id), mapOf("main" to target)),
            ),
        )
        val failure = assertFailsWith<IllegalStateException> { invalid.checkElementIds() }
        assertContains(failure.message.orEmpty(), "expectActualMappings[0].expectIds[main]: 'member'")
        assertContains(failure.message.orEmpty(), "expectActualMappings[0].actualIds[main]: 'target'")
    }

    @Test
    fun `refined elements must exist in their target fragment`() {
        assertFailsWith<IllegalStateException> {
            bundle(mapOf(id to KdRefinedElement("other"))).checkElementIds()
        }
    }

    private fun bundle(elements: Map<KdElementId, KdElement>, embedded: Map<KdElementId, KdElement> = emptyMap()): KdBundle =
        KdBundle("test", listOf(KdFragment("main", elements, embedded)), emptyList())
}
