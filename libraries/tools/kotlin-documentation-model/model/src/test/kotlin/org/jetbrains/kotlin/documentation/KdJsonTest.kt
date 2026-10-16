/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KdJsonTest {
    @Test
    fun `bundle with class, function, property, typealias, constructor, package and module round-trips through JSON`() {
        val classId = KdElementId("pkg/Foo")
        val functionId = KdElementId("pkg/Foo.bar")
        val propertyId = KdElementId("pkg/Foo.baz")
        val constructorId = KdElementId("pkg/Foo.<init>")
        val typealiasId = KdElementId("pkg/FooAlias")
        val packageId = KdElementId("pkg")
        val refinedId = KdElementId("pkg/Foo.refined")
        val inheritedId = KdElementId("pkg/Foo.inherited")

        val klass = KdClass(
            name = "Foo",
            classKind = KdClassKind.CLASS,
            isCompanion = false,
            isData = false,
            isValue = false,
            isFun = false,
            isInner = false,
            isExternal = false,
            isError = false,
            mappedTo = null,
            typeParameters = emptyList(),
            superTypes = listOf(
                KdClassLikeType(
                    qualifiers = listOf(KdClassTypeQualifier("Any", emptyList())),
                    classLikeId = KdElementId("kotlin/Any"),
                    isMarkedNullable = false,
                    annotations = emptyList(),
                ),
            ),
            declarations = listOf(functionId, propertyId, constructorId),
            visibility = KdVisibility.PUBLIC,
            modality = KdModality.FINAL,
            since = "2.4",
            deprecated = KdDeprecated(
                since = "2.3",
                level = KdDeprecationLevel.WARNING,
                replaceWith = KdReplaceWith(imports = listOf("pkg.Bar"), expression = KdExpression("Bar()", emptyList())),
            ),
            annotations = emptyList(),
        )

        val function = KdFunction(
            name = "bar",
            isCompanion = false,
            isExternal = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isInline = false,
            isTailRec = false,
            mappedTo = null,
            returnType = KdClassLikeType(emptyList(), KdElementId("kotlin/Unit"), isMarkedNullable = false, annotations = emptyList()),
            typeParameters = emptyList(),
            valueParameters = listOf(
                KdValueParameter(
                    name = "x",
                    type = KdClassLikeType(emptyList(), KdElementId("kotlin/Int"), isMarkedNullable = false, annotations = emptyList()),
                    isNoinline = false,
                    isCrossinline = false,
                    isVararg = false,
                    defaultValueExpression = KdDefaultValueExpression.Defined(KdExpression("8192", emptyList())),
                    annotations = emptyList(),
                ),
            ),
            extensionReceiverParameter = null,
            contextParameters = emptyList(),
            overridens = emptyList(),
            visibility = KdVisibility.PUBLIC,
            modality = KdModality.FINAL,
            since = null,
            deprecated = null,
            throws = listOf(KdThrows(KdElementId("kotlin/IllegalStateException"))),
            annotations = emptyList(),
        )

        val property = KdProperty(
            name = "baz",
            isInline = false,
            isCompanion = false,
            isExternal = false,
            isSuspend = false,
            isMutable = false,
            isFromPrimaryConstructor = false,
            constExpression = KdConstExpression(KdConstValue.Int(1), KdExpression("1", emptyList())),
            getter = null,
            setter = null,
            mappedTo = null,
            type = KdClassLikeType(emptyList(), KdElementId("kotlin/Int"), isMarkedNullable = false, annotations = emptyList()),
            typeParameters = emptyList(),
            extensionReceiverParameter = null,
            contextParameters = emptyList(),
            overridens = emptyList(),
            visibility = KdVisibility.PUBLIC,
            modality = KdModality.FINAL,
            throws = emptyList(),
            annotations = emptyList(),
        )

        val constructor = KdConstructor(
            isPrimary = true,
            isExternal = false,
            mappedTo = null,
            typeParameters = emptyList(),
            valueParameters = emptyList(),
            visibility = KdVisibility.PUBLIC,
            since = null,
            deprecated = null,
            throws = emptyList(),
            annotations = emptyList(),
        )

        val typeAlias = KdTypealias(
            name = "FooAlias",
            typeParameters = emptyList(),
            underlyingType = KdClassLikeType(emptyList(), classId, isMarkedNullable = false, annotations = emptyList()),
            declarations = emptyList(),
            visibility = KdVisibility.PUBLIC,
            since = null,
            deprecated = null,
            annotations = emptyList(),
        )

        val kdPackage = KdPackage(name = "pkg", declarations = listOf(classId, typealiasId), annotations = emptyList())

        val fragment = KdFragment(
            name = "commonMain",
            elements = mapOf(
                classId to klass,
                functionId to function,
                propertyId to property,
                constructorId to constructor,
                typealiasId to typeAlias,
                packageId to kdPackage,
                refinedId to KdRefinedElement("commonMain"),
                inheritedId to KdInheritedMember(classId),
            ),
            embeddedElements = mapOf(
                KdElementId("kotlin/Any") to KdClass(
                    name = "Any",
                    classKind = KdClassKind.CLASS,
                    isCompanion = false,
                    isData = false,
                    isValue = false,
                    isFun = false,
                    isInner = false,
                    isExternal = false,
                    isError = false,
                    mappedTo = null,
                    typeParameters = emptyList(),
                    superTypes = emptyList(),
                    declarations = emptyList(),
                    visibility = KdVisibility.PUBLIC,
                    modality = KdModality.OPEN,
                    since = null,
                    deprecated = null,
                    annotations = emptyList(),
                ),
            ),
        )

        val bundle = KdBundle(
            schemaVersion = "0.1.1",
            fragments = listOf(fragment),
            expectActualMappings = listOf(
                KdExpectActualMapping(
                    expectIds = mapOf("commonMain" to functionId),
                    actualIds = mapOf("jvmMain" to functionId, "nativeMain" to functionId),
                ),
            ),
        )

        val json = KdJson.encodeToString(KdBundle.serializer(), bundle)
        val decoded = KdJson.decodeFromString(KdBundle.serializer(), json)

        assertEquals(bundle.schemaVersion, decoded.schemaVersion)
        val decodedFragment = decoded.fragments.single()
        assertEquals(fragment.elements.keys, decodedFragment.elements.keys)
        assertIs<KdClass>(decodedFragment.elements.getValue(classId))
        assertIs<KdFunction>(decodedFragment.elements.getValue(functionId))
        assertIs<KdProperty>(decodedFragment.elements.getValue(propertyId))
        assertIs<KdConstructor>(decodedFragment.elements.getValue(constructorId))
        assertIs<KdTypealias>(decodedFragment.elements.getValue(typealiasId))
        assertIs<KdPackage>(decodedFragment.elements.getValue(packageId))
        assertIs<KdRefinedElement>(decodedFragment.elements.getValue(refinedId))
        assertIs<KdInheritedMember>(decodedFragment.elements.getValue(inheritedId))
        assertEquals(classId, (decodedFragment.elements.getValue(inheritedId) as KdInheritedMember).originalId)
        assertEquals(bundle.expectActualMappings, decoded.expectActualMappings)

        val decodedFunction = decodedFragment.elements.getValue(functionId) as KdFunction
        val decodedDefaultValue = decodedFunction.valueParameters.single().defaultValueExpression
        assertIs<KdDefaultValueExpression.Defined>(decodedDefaultValue)
        assertEquals("8192", decodedDefaultValue.expression.text)
    }
}
