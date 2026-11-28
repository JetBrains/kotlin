/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.valueParameterDeclarationId
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import kotlin.test.Test
import kotlin.test.assertEquals

@MustRunOnChangesInAnalysisApi
class DocumentableTest {

    @Test
    fun withDescendents() {
        val classId = KdElementId("model/TestClass")
        val function0Id = KdElementId("model/TestClass#function0#0")
        val function1Id = KdElementId("model/TestClass#function1#0")

        fun parameter(ownerId: KdElementId, index: Int, name: String) = DParameter(
            declarationId = valueParameterDeclarationId(ownerId, index),
            name = name,
            documentation = emptyMap(),
            expectPresentInSet = null,
            extra = PropertyContainer.empty(),
            sourceSets = emptySet(),
            type = Void
        )

        val dClass = DClass(
            declarationId = classId,
            name = "TestClass",
            constructors = emptyList(),
            classlikes = emptyList(),
            typealiases = emptyList(),
            companion = null,
            documentation = emptyMap(),
            expectPresentInSet = null,
            extra = PropertyContainer.empty(),
            visibility = emptyMap(),
            generics = emptyList(),
            modifier = emptyMap(),
            properties = emptyList(),
            sources = emptyMap(),
            sourceSets = emptySet(),
            supertypes = emptyMap(),
            isExpectActual = false,
            functions = listOf(
                DFunction(
                    declarationId = function0Id,
                    name = "function0",
                    documentation = emptyMap(),
                    expectPresentInSet = null,
                    extra = PropertyContainer.empty(),
                    visibility = emptyMap(),
                    generics = emptyList(),
                    modifier = emptyMap(),
                    sources = emptyMap(),
                    sourceSets = emptySet(),
                    type = Void,
                    receiver = null,
                    isConstructor = false,
                    isExpectActual = false,
                    parameters = listOf(
                        parameter(function0Id, 0, "f0p0"),
                        parameter(function0Id, 1, "f0p1")
                    )
                ),
                DFunction(
                    declarationId = function1Id,
                    name = "function1",
                    documentation = emptyMap(),
                    expectPresentInSet = null,
                    extra = PropertyContainer.empty(),
                    visibility = emptyMap(),
                    generics = emptyList(),
                    modifier = emptyMap(),
                    sources = emptyMap(),
                    sourceSets = emptySet(),
                    type = Void,
                    receiver = null,
                    isConstructor = false,
                    isExpectActual = false,
                    parameters = listOf(
                        parameter(function1Id, 0, "f1p0"),
                        parameter(function1Id, 1, "f1p1")
                    )
                )
            )
        )

        assertEquals(
            listOf("TestClass", "function0", "f0p0", "f0p1", "function1", "f1p0", "f1p1"),
            dClass.withDescendants().map { it.name }.toList()
        )
    }
}
