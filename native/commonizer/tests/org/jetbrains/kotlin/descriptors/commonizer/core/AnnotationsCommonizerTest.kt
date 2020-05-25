/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirAnnotationFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
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
        listOf(mockAnnotation("org.sample.Foo"))
    )

    @Test
    fun noRelevantAnnotations2() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org.sample.Foo")),
        emptyList(),
        emptyList()
    )

    @Test
    fun noRelevantAnnotations3() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org.sample.Foo")),
        listOf(mockAnnotation("org.sample.Foo")),
        listOf(mockAnnotation("org.sample.Foo"))
    )

    @Test
    fun noRelevantAnnotations4() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("org.sample.Foo")),
        listOf(mockAnnotation("org.sample.Bar")),
        listOf(mockAnnotation("org.sample.Baz"))
    )

    @Test
    fun noRelevantAnnotations5() = doTestSuccess(
        expected = emptyList(),
        listOf(mockAnnotation("kotlin.PublishedApi")),
        listOf(mockAnnotation("kotlin.PublishedApi")),
        listOf(mockAnnotation("kotlin.PublishedApi"))
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
    fqName: String,
    constantValueArguments: Map<Name, ConstantValue<*>> = emptyMap(),
    annotationValueArguments: Map<Name, CirAnnotation> = emptyMap()
): CirAnnotation = CirAnnotationFactory.create(
    fqName = FqName(fqName),
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
            fqName = "kotlin.ReplaceWith",
            constantValueArguments = mapOf(
                Name.identifier("expression") to StringValue(replaceWithExpression),
                Name.identifier("imports") to ArrayValue(replaceWithImports.map(::StringValue)) {
                    it.builtIns.getArrayElementType(it.builtIns.stringType)
                }
            ),
            annotationValueArguments = emptyMap()
        )
    } else null

    return mockAnnotation(
        fqName = "kotlin.Deprecated",
        constantValueArguments = HashMap<Name, ConstantValue<*>>().apply {
            this[Name.identifier("message")] = StringValue(message)

            if (level != WARNING)
                this[Name.identifier("level")] = EnumValue(
                    ClassId.topLevel(FqName("kotlin.DeprecationLevel")),
                    Name.identifier(level.name)
                )
        },
        annotationValueArguments = if (replaceWith != null) mapOf(Name.identifier("replaceWith") to replaceWith) else emptyMap()
    )
}
