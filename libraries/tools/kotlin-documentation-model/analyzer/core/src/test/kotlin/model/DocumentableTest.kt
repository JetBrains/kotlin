package model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentableTest {

    @Test
    fun withDescendents() {
        val dClass = DClass(
            dri = DRI(),
            name = "TestClass",
            constructors = emptyList(),
            classlikes = emptyList(),
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
            functions = listOf(
                DFunction(
                    dri = DRI(),
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
                    parameters = listOf(
                        DParameter(
                            dri = DRI(),
                            name = "f0p0",
                            documentation = emptyMap(),
                            expectPresentInSet = null,
                            extra = PropertyContainer.empty(),
                            sourceSets = emptySet(),
                            type = Void
                        ),
                        DParameter(
                            dri = DRI(),
                            name = "f0p1",
                            documentation = emptyMap(),
                            expectPresentInSet = null,
                            extra = PropertyContainer.empty(),
                            sourceSets = emptySet(),
                            type = Void
                        )
                    )
                ),
                DFunction(
                    dri = DRI(),
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
                    parameters = listOf(
                        DParameter(
                            dri = DRI(),
                            name = "f1p0",
                            documentation = emptyMap(),
                            expectPresentInSet = null,
                            extra = PropertyContainer.empty(),
                            sourceSets = emptySet(),
                            type = Void
                        ),
                        DParameter(
                            dri = DRI(),
                            name = "f1p1",
                            documentation = emptyMap(),
                            expectPresentInSet = null,
                            extra = PropertyContainer.empty(),
                            sourceSets = emptySet(),
                            type = Void
                        )
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