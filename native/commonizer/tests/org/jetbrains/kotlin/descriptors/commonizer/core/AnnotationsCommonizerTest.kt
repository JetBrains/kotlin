/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.junit.Test
import kotlin.DeprecationLevel.*

class AnnotationsCommonizerTest : AbstractCommonizerTest<List<CirAnnotation>, List<CirAnnotation>>() {

    @Test
    fun noAnnotations() = doTestSuccess(
        expected = emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    @Test
    fun noRelevantAnnotations1() = doTestSuccess(
        expected = emptyList(),
        emptyList(),
        emptyList(),
        listOf(mockAnnotation("org/sample/Foo"))
    )

    @Test
    fun noRelevantAnnotations2() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org/sample/Foo")),
        emptyList(),
        emptyList()
    )

    @Test
    fun noRelevantAnnotations3() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org/sample/Foo")),
        listOf(mockAnnotation("org/sample/Foo")),
        listOf(mockAnnotation("org/sample/Foo"))
    )

    @Test
    fun noRelevantAnnotations4() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org/sample/Foo")),
        listOf(mockAnnotation("org/sample/Bar")),
        listOf(mockAnnotation("org/sample/Baz"))
    )

    @Test
    fun noRelevantAnnotations5() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("kotlin/PublishedApi")),
        listOf(mockAnnotation("kotlin/PublishedApi")),
        listOf(mockAnnotation("kotlin/PublishedApi"))
    )

    @Test
    fun sameMessages() = doTestSuccess(
        expected = listOf(mockDeprecated(message = "please don't use it because ...")),
        listOf(mockDeprecated(message = "please don't use it because ...")),
        listOf(mockDeprecated(message = "please don't use it because ...")),
        listOf(mockDeprecated(message = "please don't use it because ..."))
    )

    @Test
    fun differentMessages() = doTestSuccess(
        expected = listOf(mockDeprecated(message = AnnotationsCommonizer.FALLBACK_MESSAGE)),
        listOf(mockDeprecated(message = "please don't use it because ...")),
        listOf(mockDeprecated(message = "it should not be used due to ...")),
        listOf(mockDeprecated(message = "please don't use it because ..."))
    )

    @Test
    fun sameLevels1() = doTestSuccess(
        expected = listOf(mockDeprecated(level = WARNING)),
        listOf(mockDeprecated(level = WARNING)),
        listOf(mockDeprecated(level = WARNING)),
        listOf(mockDeprecated(level = WARNING))
    )

    @Test
    fun sameLevels2() = doTestSuccess(
        expected = listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = ERROR))
    )

    @Test
    fun sameLevels3() = doTestSuccess(
        expected = listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = HIDDEN))
    )

    @Test
    fun differentLevels1() = doTestSuccess(
        expected = listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = WARNING)),
        listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = WARNING))
    )

    @Test
    fun differentLevels2() = doTestSuccess(
        expected = listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = WARNING)),
        listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = WARNING))
    )

    @Test
    fun differentLevels3() = doTestSuccess(
        expected = listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = ERROR))
    )

    @Test
    fun differentLevels4() = doTestSuccess(
        expected = listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = ERROR)),
        listOf(mockDeprecated(level = HIDDEN)),
        listOf(mockDeprecated(level = WARNING))
    )

    @Test
    fun sameReplaceWith1() = doTestSuccess(
        expected = listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = emptyArray()
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = emptyArray()
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = emptyArray()
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = emptyArray()
            )
        )
    )

    @Test
    fun sameReplaceWith2() = doTestSuccess(
        expected = listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo")
            )
        )
    )

    @Test
    fun sameReplaceWith3() = doTestSuccess(
        expected = listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        )
    )

    @Test
    fun differentReplaceWith1() = doTestSuccess(
        expected = listOf(
            mockDeprecated(
                replaceWithExpression = "",
                replaceWithImports = emptyArray()
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Bar",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        )
    )

    @Test
    fun differentReplaceWith2() = doTestSuccess(
        expected = listOf(
            mockDeprecated(
                replaceWithExpression = "",
                replaceWithImports = emptyArray()
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo")
            )
        ),
        listOf(
            mockDeprecated(
                replaceWithExpression = "org.sample.Foo",
                replaceWithImports = arrayOf("org.sample.Foo", "org.sample.foo.*")
            )
        )
    )

    override fun createCommonizer() = AnnotationsCommonizer()
}

private fun mockAnnotation(
    classId: String,
    constantValueArguments: Map<CirName, CirConstantValue<*>> = emptyMap(),
    annotationValueArguments: Map<CirName, CirAnnotation> = emptyMap()
): CirAnnotation = CirAnnotation.createInterned(
    type = mockClassType(classId),
    constantValueArguments = constantValueArguments,
    annotationValueArguments = annotationValueArguments
)

private fun mockDeprecated(
    message: String = "",
    level: DeprecationLevel = WARNING,
    replaceWithExpression: String = "",
    replaceWithImports: Array<String> = emptyArray()
): CirAnnotation {
    val replaceWith: CirAnnotation? = if (replaceWithExpression.isNotEmpty() || replaceWithImports.isNotEmpty()) {
        mockAnnotation(
            classId = "kotlin/ReplaceWith",
            constantValueArguments = mapOf(
                CirName.create("expression") to StringValue(replaceWithExpression),
                CirName.create("imports") to ArrayValue(replaceWithImports.map(::StringValue))
            ),
            annotationValueArguments = emptyMap()
        )
    } else null

    return mockAnnotation(
        classId = "kotlin/Deprecated",
        constantValueArguments = HashMap<CirName, CirConstantValue<*>>().apply {
            this[CirName.create("message")] = StringValue(message)

            if (level != WARNING)
                this[CirName.create("level")] = EnumValue(CirEntityId.create("kotlin/DeprecationLevel"), CirName.create(level.name))
        },
        annotationValueArguments = if (replaceWith != null) mapOf(CirName.create("replaceWith") to replaceWith) else emptyMap()
    )
}
